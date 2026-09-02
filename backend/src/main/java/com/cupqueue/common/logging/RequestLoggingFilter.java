package com.cupqueue.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Establishes request-scoped log correlation and records one HTTP completion event.
 *
 * <p>The filter accepts a syntactically safe request identifier from {@value #REQUEST_ID_HEADER}
 * or generates one when the header is absent or invalid. The identifier is returned in the
 * response and remains in the MDC for the duration of request processing. Request and response
 * bodies, query strings, credentials, and cookies are never logged.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    /** HTTP header used to receive and return the request correlation identifier. */
    public static final String REQUEST_ID_HEADER = "X-Request-ID";

    /** MDC key under which the request correlation identifier is stored. */
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    /**
     * Creates the request logging filter.
     */
    public RequestLoggingFilter() {
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));
        long startedAt = System.nanoTime();
        boolean failed = false;

        response.setHeader(REQUEST_ID_HEADER, requestId);

        try (MDC.MDCCloseable ignored = MDC.putCloseable(REQUEST_ID_MDC_KEY, requestId)) {
            try {
                filterChain.doFilter(request, response);
            } catch (ServletException | IOException | RuntimeException exception) {
                failed = true;
                throw exception;
            } finally {
                if (shouldLogRequest(request)) {
                    int status = failed && response.getStatus() < 500
                            ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                            : response.getStatus();
                    long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
                    logRequest(request, status, durationMs);
                }
            }
        }
    }

    private String resolveRequestId(String candidate) {
        if (candidate != null && VALID_REQUEST_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }

    private boolean shouldLogRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/actuator/health")
                && !path.equals("/actuator/info")
                && !path.startsWith("/swagger-ui")
                && !path.startsWith("/v3/api-docs");
    }

    private void logRequest(HttpServletRequest request, int status, long durationMs) {
        if (status >= 500) {
            log.atError()
                    .addKeyValue("event", "http.request")
                    .addKeyValue("method", request.getMethod())
                    .addKeyValue("path", request.getRequestURI())
                    .addKeyValue("status", status)
                    .addKeyValue("durationMs", durationMs)
                    .log("HTTP request completed");
            return;
        }

        log.atInfo()
                .addKeyValue("event", "http.request")
                .addKeyValue("method", request.getMethod())
                .addKeyValue("path", request.getRequestURI())
                .addKeyValue("status", status)
                .addKeyValue("durationMs", durationMs)
                .log("HTTP request completed");
    }
}
