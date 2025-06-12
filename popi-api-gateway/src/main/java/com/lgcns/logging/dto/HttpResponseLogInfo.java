package com.lgcns.logging.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import org.slf4j.MDC;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;

@Builder
public record HttpResponseLogInfo(String traceId, String responseBody, Integer responseStatus) {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static HttpResponseLogInfo from(
            byte[] bodyBytes, HttpStatusCode status, String contentType) {
        String traceId = MDC.get("traceId");
        String responseBody = getContent(contentType, bodyBytes);
        return HttpResponseLogInfo.builder()
                .traceId(traceId)
                .responseBody(responseBody)
                .responseStatus(status != null ? status.value() : null)
                .build();
    }

    private static String getContent(String contentType, byte[] content) {
        boolean visible =
                isVisible(
                        MediaType.valueOf(contentType == null ? "application/json" : contentType));
        if (visible) {
            if (content.length > 0) {
                return new String(
                        content, 0, Math.min(content.length, 5120), StandardCharsets.UTF_8);
            } else {
                return "";
            }
        } else {
            return "BINARY";
        }
    }

    private static boolean isVisible(MediaType mediaType) {
        final List<MediaType> VISIBLE_TYPES =
                Arrays.asList(
                        MediaType.valueOf("text/*"),
                        MediaType.APPLICATION_FORM_URLENCODED,
                        MediaType.APPLICATION_JSON,
                        MediaType.APPLICATION_XML,
                        MediaType.valueOf("application/*+json"),
                        MediaType.valueOf("application/*+xml"),
                        MediaType.MULTIPART_FORM_DATA);
        return VISIBLE_TYPES.stream().anyMatch(visibleType -> visibleType.includes(mediaType));
    }

    public String toJson() {
        try {
            return objectMapper.writeValueAsString(this).replace("\\", "");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
