package com.lgcns.logging.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import org.slf4j.MDC;
import org.springframework.http.server.reactive.ServerHttpRequest;

@Builder
public record HttpRequestLogInfo(
        String traceId,
        String requestMethod,
        String requestUri,
        String xAmznTraceId,
        String userAgent) {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static HttpRequestLogInfo from(ServerHttpRequest request) {
        String queryString = request.getURI().getQuery();
        String traceId = MDC.get("traceId");
        String requestMethod = request.getMethod().toString();
        String requestUri =
                request.getURI().getPath() + (queryString == null ? "" : "?" + queryString);
        String xAmznTraceId = request.getHeaders().getFirst("x-amzn-trace-id");
        String userAgent = request.getHeaders().getFirst("user-agent");

        return HttpRequestLogInfo.builder()
                .traceId(traceId)
                .requestMethod(requestMethod)
                .requestUri(requestUri)
                .xAmznTraceId(xAmznTraceId)
                .userAgent(userAgent)
                .build();
    }
}
