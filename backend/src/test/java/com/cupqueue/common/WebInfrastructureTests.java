package com.cupqueue.common;

import com.cupqueue.common.exception.BusinessException;
import com.cupqueue.common.exception.CommonErrorCode;
import com.cupqueue.common.logging.RequestLoggingFilter;
import com.cupqueue.common.web.GlobalExceptionHandler;
import com.cupqueue.common.web.ProblemDetailFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebInfrastructureTests {

    private static final String CLIENT_REQUEST_ID = "client_request-123";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProblemDetailFactory problemDetailFactory = new ProblemDetailFactory();
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(problemDetailFactory);
        mockMvc = MockMvcBuilders.standaloneSetup(new InfrastructureTestController())
                .setControllerAdvice(exceptionHandler)
                .addFilters(new RequestLoggingFilter())
                .build();
    }

    @Test
    void returnsConsistentProblemDetailForBusinessException() throws Exception {
        mockMvc.perform(get("/infrastructure-test/business-error")
                        .header(RequestLoggingFilter.REQUEST_ID_HEADER, CLIENT_REQUEST_ID))
                .andExpect(status().isConflict())
                .andExpect(header().string(RequestLoggingFilter.REQUEST_ID_HEADER, CLIENT_REQUEST_ID))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("DATA_CONFLICT"))
                .andExpect(jsonPath("$.detail").value("Test conflict"))
                .andExpect(jsonPath("$.instance").value("/infrastructure-test/business-error"))
                .andExpect(jsonPath("$.requestId").value(CLIENT_REQUEST_ID));

        assertNull(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY));
    }

    @Test
    void reportsRequestBodyValidationWithoutEchoingRejectedValue() throws Exception {
        mockMvc.perform(post("/infrastructure-test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(
                        RequestLoggingFilter.REQUEST_ID_HEADER,
                        matchesPattern("[0-9a-f-]{36}")
                ))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(content().string(not(containsString("rejectedValue"))));

        assertNull(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY));
    }

    @Test
    void rejectsMalformedJsonWithSafeProblemDetail() throws Exception {
        mockMvc.perform(post("/infrastructure-test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.detail").value("The request body is invalid"));
    }

    @Test
    void hidesUnexpectedExceptionDetails() throws Exception {
        mockMvc.perform(get("/infrastructure-test/unexpected-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("sensitive internal detail"))));
    }

    @Test
    void replacesUnsafeRequestId() throws Exception {
        String requestId = mockMvc.perform(get("/infrastructure-test/ok")
                        .header(RequestLoggingFilter.REQUEST_ID_HEADER, "unsafe request id"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        RequestLoggingFilter.REQUEST_ID_HEADER,
                        matchesPattern("[0-9a-f-]{36}")
                ))
                .andReturn()
                .getResponse()
                .getHeader(RequestLoggingFilter.REQUEST_ID_HEADER);

        assertEquals(36, requestId.length());
        assertNull(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY));
    }

    @RestController
    @RequestMapping("/infrastructure-test")
    static class InfrastructureTestController {

        @GetMapping("/ok")
        String ok() {
            return "ok";
        }

        @GetMapping("/business-error")
        void businessError() {
            throw new BusinessException(CommonErrorCode.DATA_CONFLICT, "Test conflict");
        }

        @GetMapping("/unexpected-error")
        void unexpectedError() {
            throw new IllegalStateException("sensitive internal detail");
        }

        @PostMapping("/validation")
        void validate(@Valid @RequestBody TestRequest request) {
        }
    }

    record TestRequest(@NotBlank String name) {
    }
}
