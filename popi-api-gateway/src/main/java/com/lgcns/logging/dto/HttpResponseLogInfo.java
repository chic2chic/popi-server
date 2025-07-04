package com.lgcns.logging.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import org.slf4j.MDC;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;

@Builder
public record HttpResponseLogInfo(String traceId, Object responseBody, Integer responseStatus) {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static HttpResponseLogInfo from(
            byte[] bodyBytes, HttpStatusCode status, String contentType) {
        String traceId = MDC.get("traceId");
        Object responseBody = getContent(contentType, bodyBytes);
        return HttpResponseLogInfo.builder()
                .traceId(traceId)
                .responseBody(responseBody)
                .responseStatus(status != null ? status.value() : null)
                .build();
    }

    private static Object getContent(String contentType, byte[] content) {
        boolean visible =
                isVisible(
                        MediaType.valueOf(contentType == null ? "application/json" : contentType));
        if (visible) {
            if (content != null && content.length > 0) {
                try {
                    return new ObjectMapper().readValue(content, Object.class);
                } catch (IOException e) {
                    return new String(content, StandardCharsets.UTF_8);
                }
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
}
