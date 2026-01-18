package com.example.demo.system;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    private static final int MAX_PAYLOAD_LENGTH = 10000; // 10KB limit

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Optionally skip logging for certain paths
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health") || path.startsWith("/actuator/prometheus");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Wrap request and response to allow reading body multiple times
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        try {
            // Set MDC with contextual info only
            LoggingContext.put("requestId", requestId);
            LoggingContext.put("uri", request.getRequestURI());
            LoggingContext.put("method", request.getMethod());

            // Log incoming request
            logRequest(requestWrapper, requestId);

            // Process the request
            filterChain.doFilter(requestWrapper, responseWrapper);

        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // Log response
            logResponse(responseWrapper, requestId, duration);

            // IMPORTANT: Copy response body back to original response
            responseWrapper.copyBodyToResponse();

            // Get as ordered map if needed
            Map<String, Object> orderedContext = LoggingContext.asMap();
            System.out.println(orderedContext);

            // CRITICAL: Always clear to prevent memory leaks
            if (!isAsyncStarted(request)) {
                LoggingContext.clear();
            }
        }
    }

    private void logRequest(ContentCachingRequestWrapper request, String requestId) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String queryString = request.getQueryString();
        String clientIp = getClientIP(request);

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n========== Incoming Request ==========\n");
        logMessage.append("Request ID: ").append(requestId).append("\n");
        logMessage.append("URI: ").append(uri);
        if (queryString != null) {
            logMessage.append("?").append(queryString);
        }
        logMessage.append("\n");
        logMessage.append("Method: ").append(method).append("\n");
        logMessage.append("Client IP: ").append(clientIp).append("\n");
        logMessage.append("Headers: ").append(getHeaders(request)).append("\n");

        LoggingContext.put("method", method);
        LoggingContext.put("ClientIP", clientIp);
        LoggingContext.put("Headers", getHeaders(request));

        // Log request body if present
        String requestBody = getRequestBody(request);
        if (requestBody != null && !requestBody.isEmpty()) {
            logMessage.append("Request Body: ").append(requestBody).append("\n");
            LoggingContext.put("requestBody", requestBody);
        }
        logMessage.append("=====================================");

        log.info(logMessage.toString());
    }

    private void logResponse(
            ContentCachingResponseWrapper response, String requestId, long duration) {

        int status = response.getStatus();

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n========== Outgoing Response =========\n");
        logMessage.append("Request ID: ").append(requestId).append("\n");
        logMessage.append("Status Code: ").append(status).append("\n");
        logMessage.append("Duration: ").append(duration).append("ms\n");
        logMessage.append("Headers: ").append(getResponseHeaders(response)).append("\n");

        LoggingContext.put("StatusCode", String.valueOf(status));
        LoggingContext.put("Duration", String.format("%.3f seconds", duration / 1000.0));
        LoggingContext.put("Response Headers", getResponseHeaders(response));

        // Log response body
        String responseBody = getResponseBody(response);
        LoggingContext.put("responseBody", responseBody);
        if (responseBody != null && !responseBody.isEmpty()) {
            logMessage.append("Response Body: ").append(responseBody).append("\n");
        }
        logMessage.append("=====================================");

        // Use different log levels based on status code
        if (status >= 500) {
            log.error(logMessage.toString());
        } else if (status >= 400) {
            log.warn(logMessage.toString());
        } else {
            log.info(logMessage.toString());
        }
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            String body = new String(content, StandardCharsets.UTF_8);
            return body.length() > MAX_PAYLOAD_LENGTH
                    ? body.substring(0, MAX_PAYLOAD_LENGTH) + "... (truncated)"
                    : body;
        }
        return null;
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            String body = new String(content, StandardCharsets.UTF_8);
            return body.length() > MAX_PAYLOAD_LENGTH
                    ? body.substring(0, MAX_PAYLOAD_LENGTH) + "... (truncated)"
                    : body;
        }
        return null;
    }

    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String getHeaders(HttpServletRequest request) {
        StringBuilder headers = new StringBuilder();
        request.getHeaderNames()
                .asIterator()
                .forEachRemaining(
                        headerName -> {
                            // Skip sensitive headers
                            if (!headerName.equalsIgnoreCase("Authorization")
                                    && !headerName.equalsIgnoreCase("Cookie")) {
                                headers.append(headerName)
                                        .append(": ")
                                        .append(request.getHeader(headerName))
                                        .append("; ");
                            }
                        });
        return headers.toString();
    }

    private String getResponseHeaders(ContentCachingResponseWrapper response) {
        StringBuilder headers = new StringBuilder();
        response.getHeaderNames()
                .forEach(
                        headerName -> {
                            headers.append(headerName)
                                    .append(": ")
                                    .append(response.getHeader(headerName))
                                    .append("; ");
                        });
        return headers.toString();
    }
}
