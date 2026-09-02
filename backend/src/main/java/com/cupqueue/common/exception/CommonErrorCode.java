package com.cupqueue.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Defines infrastructure-level errors shared by all API modules.
 *
 * <p>Errors owned by a business module should be declared in that module rather than added
 * to this enumeration.</p>
 */
public enum CommonErrorCode implements ErrorCode {

    /** A request that cannot be processed because its input or structure is invalid. */
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "The request is invalid"),

    /** A request whose validated fields or method parameters contain invalid values. */
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Request validation failed"),

    /** A request body that cannot be parsed into the required representation. */
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "The request body is invalid"),

    /** A requested HTTP resource that does not exist. */
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested resource was not found"),

    /** An operation rejected by a persistent data constraint. */
    DATA_CONFLICT(HttpStatus.CONFLICT, "The operation conflicts with existing data"),

    /** An unexpected server-side failure that must not expose implementation details. */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");

    private final HttpStatus status;
    private final String defaultMessage;

    CommonErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }
}
