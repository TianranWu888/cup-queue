package com.cupqueue.common.web;

import com.cupqueue.common.exception.BusinessException;
import com.cupqueue.common.exception.CommonErrorCode;
import com.cupqueue.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates application and Spring MVC exceptions into safe, consistent API responses.
 *
 * <p>Expected request failures are returned without stack traces. Unexpected failures are logged
 * with their stack trace while clients receive only the generic internal-error message.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ProblemDetailFactory problemDetailFactory;

    /**
     * Creates the global exception handler.
     *
     * @param problemDetailFactory factory used to build client-facing problem details
     */
    public GlobalExceptionHandler(ProblemDetailFactory problemDetailFactory) {
        this.problemDetailFactory = problemDetailFactory;
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ProblemDetail> handleBusinessException(
            BusinessException exception,
            WebRequest request
    ) {
        ErrorCode errorCode = exception.getErrorCode();
        log.atInfo()
                .addKeyValue("event", "request.rejected")
                .addKeyValue("errorCode", errorCode.code())
                .log("Business request rejected");

        ProblemDetail problem = problemDetailFactory.create(errorCode, exception.getMessage(), request);
        return ResponseEntity.status(errorCode.status()).body(problem);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            WebRequest request
    ) {
        log.atWarn()
                .addKeyValue("event", "data.conflict")
                .addKeyValue("exceptionType", exception.getClass().getSimpleName())
                .log("Data integrity constraint rejected the request");

        ProblemDetail problem = problemDetailFactory.create(
                CommonErrorCode.DATA_CONFLICT,
                CommonErrorCode.DATA_CONFLICT.defaultMessage(),
                request
        );
        return ResponseEntity.status(CommonErrorCode.DATA_CONFLICT.status()).body(problem);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpectedException(Exception exception, WebRequest request) {
        log.error("Unhandled request failure", exception);
        ProblemDetail problem = problemDetailFactory.create(
                CommonErrorCode.INTERNAL_ERROR,
                CommonErrorCode.INTERNAL_ERROR.defaultMessage(),
                request
        );
        return ResponseEntity.status(CommonErrorCode.INTERNAL_ERROR.status()).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> validationError(error.getField(), error.getDefaultMessage()))
                .toList();
        ProblemDetail problem = problemDetailFactory.create(
                status,
                CommonErrorCode.VALIDATION_FAILED,
                CommonErrorCode.VALIDATION_FAILED.defaultMessage(),
                request
        );
        problem.setProperty("errors", errors);
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        List<String> errors = exception.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .toList();
        ProblemDetail problem = problemDetailFactory.create(
                status,
                CommonErrorCode.VALIDATION_FAILED,
                CommonErrorCode.VALIDATION_FAILED.defaultMessage(),
                request
        );
        problem.setProperty("errors", errors);
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        ProblemDetail problem = problemDetailFactory.create(
                status,
                CommonErrorCode.INVALID_REQUEST_BODY,
                CommonErrorCode.INVALID_REQUEST_BODY.defaultMessage(),
                request
        );
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        ProblemDetail problem = problemDetailFactory.create(
                status,
                CommonErrorCode.RESOURCE_NOT_FOUND,
                CommonErrorCode.RESOURCE_NOT_FOUND.defaultMessage(),
                request
        );
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        Object responseBody = body;
        if (!(body instanceof ProblemDetail problem) || problem.getProperties() == null
                || !problem.getProperties().containsKey("code")) {
            CommonErrorCode errorCode = status.is5xxServerError()
                    ? CommonErrorCode.INTERNAL_ERROR
                    : CommonErrorCode.INVALID_REQUEST;
            responseBody = problemDetailFactory.create(
                    status,
                    errorCode,
                    errorCode.defaultMessage(),
                    request
            );
        }
        if (status.is5xxServerError()) {
            log.error("Unhandled framework request failure", exception);
        }
        return super.handleExceptionInternal(exception, responseBody, headers, status, request);
    }

    private Map<String, String> validationError(String field, String message) {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("field", field);
        error.put("message", message == null ? "Invalid value" : message);
        return error;
    }
}
