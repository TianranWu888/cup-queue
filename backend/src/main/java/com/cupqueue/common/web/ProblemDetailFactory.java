package com.cupqueue.common.web;

import com.cupqueue.common.exception.ErrorCode;
import com.cupqueue.common.logging.RequestLoggingFilter;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.util.Objects;

/**
 * Creates consistent RFC 9457 problem details for the HTTP boundary.
 *
 * <p>Every created response contains a stable application error code. When a request identifier
 * is available in the MDC, it is included so clients can correlate the response with server
 * logs.</p>
 */
@Component
public class ProblemDetailFactory {

    /**
     * Creates the stateless problem-detail factory.
     */
    public ProblemDetailFactory() {
    }

    /**
     * Creates a problem detail using the status declared by an error code.
     *
     * @param errorCode the stable error contract
     * @param detail a safe client-facing detail, or {@code null} to use the default message
     * @param request the current web request
     * @return a problem detail containing the error code and available request identifier
     * @throws NullPointerException if {@code errorCode} is {@code null}
     */
    public ProblemDetail create(ErrorCode errorCode, String detail, WebRequest request) {
        Objects.requireNonNull(errorCode, "errorCode must not be null");
        return create(errorCode.status(), errorCode, detail, request);
    }

    /**
     * Creates a problem detail using an explicit response status.
     *
     * <p>This overload is used when Spring MVC determines the status for a framework exception
     * while CupQueue supplies the stable error code and safe response detail.</p>
     *
     * @param status the HTTP response status determined by the web framework
     * @param errorCode the stable error contract
     * @param detail a safe client-facing detail, or {@code null} to use the default message
     * @param request the current web request
     * @return a problem detail containing the error code and available request identifier
     * @throws NullPointerException if {@code status} or {@code errorCode} is {@code null}
     */
    public ProblemDetail create(
            HttpStatusCode status,
            ErrorCode errorCode,
            String detail,
            WebRequest request
    ) {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(errorCode, "errorCode must not be null");

        String resolvedDetail = detail == null || detail.isBlank()
                ? errorCode.defaultMessage()
                : detail;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, resolvedDetail);
        HttpStatus resolvedStatus = HttpStatus.resolve(status.value());
        if (resolvedStatus != null) {
            problem.setTitle(resolvedStatus.getReasonPhrase());
        }
        if (request instanceof ServletWebRequest servletWebRequest) {
            problem.setInstance(URI.create(servletWebRequest.getRequest().getRequestURI()));
        }
        problem.setProperty("code", errorCode.code());

        String requestId = MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY);
        if (requestId != null) {
            problem.setProperty("requestId", requestId);
        }
        return problem;
    }
}
