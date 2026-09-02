package com.cupqueue.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Defines the stable client-facing contract for an application error.
 *
 * <p>An error code supplies the machine-readable identifier, HTTP status, and safe default
 * message used when the HTTP layer creates a problem-detail response.</p>
 */
public interface ErrorCode {

    /**
     * Returns the stable identifier intended for programmatic client handling.
     *
     * @return the machine-readable error code
     */
    String code();

    /**
     * Returns the HTTP status associated with the error.
     *
     * @return the response status
     */
    HttpStatus status();

    /**
     * Returns a safe message suitable for a client response.
     *
     * @return the default client-facing message
     */
    String defaultMessage();
}
