package com.example.dop.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;

public class RequestResponseLoggingInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(RequestResponseLoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestBody = getRequestBody(request);
        logger.info("Request: {} {} - Body: {}", request.getMethod(), request.getRequestURI(), requestBody);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute("startTime");
        long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;

        String responseBody = getResponseBody(response);
        String contentType = response.getContentType();

        logger.info("Response: {} - Status: {} - Duration: {}ms - Content-Type: {} - Body: {}",
                request.getRequestURI(),
                response.getStatus(),
                duration,
                contentType,
                responseBody);

        if (ex != null) {
            logger.error("Exception occurred during request processing: ", ex);
        }
    }

    private String getRequestBody(HttpServletRequest request) {
        // Implementation to read request body
        // Note: You'll need ContentCachingRequestWrapper for this
        return "";
    }

    private String getResponseBody(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper) {
            ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) response;
            byte[] content = wrapper.getContentAsByteArray();
            if (content.length > 0) {
                String contentType = response.getContentType();

                // Only log text-based content types
                if (isLoggableContentType(contentType)) {
                    try {
                        return new String(content, StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        return "[Error reading response body: " + e.getMessage() + "]";
                    }
                } else {
                    return "[Binary content - " + content.length + " bytes]";
                }
            }
        }
        return "[No response body]";
    }

    private boolean isLoggableContentType(String contentType) {
        if (contentType == null) {
            return false;
        }

        String lowerContentType = contentType.toLowerCase();
        return lowerContentType.contains("application/json") ||
                lowerContentType.contains("application/xml") ||
                lowerContentType.contains("text/") ||
                lowerContentType.contains("application/x-www-form-urlencoded");
    }
}
