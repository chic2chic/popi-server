package com.lgcns.logging.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
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

    // WebFlux용 from 메서드
    public static HttpRequestLogInfo from(ServerHttpRequest request) {
        String traceId = MDC.get("traceId");
        String requestMethod = request.getMethod().toString();
        String requestUri = request.getURI().toString();
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

    public String toJson() {
        try {
            return objectMapper.writeValueAsString(this).replace("\\", "");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
