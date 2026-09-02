package com.cupqueue.common.exception;

import java.util.Objects;

/**
 * Represents an expected rejection caused by an application rule or request state.
 *
 * <p>This exception carries a stable {@link ErrorCode} so the HTTP boundary can produce a
 * consistent response. It should not be used for unexpected programming or infrastructure
 * failures.</p>
 */
public class BusinessException extends RuntimeException {

    /** Stable error contract used to translate this exception at the HTTP boundary. */
    private final ErrorCode errorCode;

    /**
     * Creates an exception using the error code's default client-facing message.
     *
     * @param errorCode the error contract associated with the rejection
     * @throws NullPointerException if {@code errorCode} is {@code null}
     */
    public BusinessException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    /**
     * Creates an exception with an optional client-facing message override.
     *
     * @param errorCode the error contract associated with the rejection
     * @param message the safe client-facing message, or {@code null} to use the default
     * @throws NullPointerException if {@code errorCode} is {@code null}
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(resolveMessage(errorCode, message));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    /**
     * Returns the error contract associated with this exception.
     *
     * @return the non-null error code
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    private static String resolveMessage(ErrorCode errorCode, String message) {
        Objects.requireNonNull(errorCode, "errorCode must not be null");
        return message == null || message.isBlank() ? errorCode.defaultMessage() : message;
    }
}
