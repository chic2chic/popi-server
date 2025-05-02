package com.lgcns.error;

import java.time.LocalDateTime;

public record FailResponse(
        boolean success, int status, ErrorResponse data, LocalDateTime timestamp) {
    public static FailResponse of(int status, ErrorResponse errorResponse) {
        return new FailResponse(false, status, errorResponse, LocalDateTime.now());
    }
}
