package com.kirandev.sharedlogginglib;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Populates MDC for HTTP requests using W3C Trace Context and B3 headers so logs
 * correlate with Dynatrace, Splunk, Grafana, and OpenTelemetry collectors.
 */
public class MdcFilter extends OncePerRequestFilter {

    private static final String B3_TRACE_ID = "X-B3-TraceId";
    private static final String B3_SPAN_ID = "X-B3-SpanId";
    private static final String W3C_TRACEPARENT = "traceparent";
    private static final String REQUEST_ID = "X-Request-Id";
    private static final String CORRELATION_ID = "X-Correlation-Id";

    @Value("${spring.application.name:service}")
    private String serviceName;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            MDC.put(LoggingMdc.SERVICE_ID, serviceName);

            String traceId = resolveTraceId(request);
            MDC.put(LoggingMdc.TRACE_ID, traceId);
            MDC.put(LoggingMdc.SPAN_ID, getOrGenerate(request, B3_SPAN_ID));

            MDC.put(LoggingMdc.REQUEST_ID, getOrGenerate(request, REQUEST_ID));
            MDC.put(LoggingMdc.CORRELATION_ID, getOrGenerate(request, CORRELATION_ID));

            MDC.put(LoggingMdc.HTTP_METHOD, request.getMethod());
            MDC.put(LoggingMdc.HTTP_PATH, sanitisePath(request.getRequestURI()));

            response.setHeader("X-B3-TraceId", traceId);
            response.setHeader("X-Request-Id", MDC.get(LoggingMdc.REQUEST_ID));

            filterChain.doFilter(request, response);

        } finally {
            MDC.clear();
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceparent = request.getHeader(W3C_TRACEPARENT);
        if (traceparent != null && !traceparent.isBlank()) {
            String[] parts = traceparent.split("-");
            if (parts.length >= 2) {
                return parts[1];
            }
        }
        return getOrGenerate(request, B3_TRACE_ID);
    }

    private String getOrGenerate(HttpServletRequest request, String header) {
        String value = request.getHeader(header);
        return (value != null && !value.isBlank())
                ? value
                : UUID.randomUUID().toString().replace("-", "");
    }

    private String sanitisePath(String uri) {
        if (uri == null) {
            return "unknown";
        }
        return uri
                .replaceAll("/[0-9a-fA-F\\-]{8,}", "/{id}")
                .replaceAll("/\\d+", "/{id}");
    }
}
