package com.lgcns.response;

import java.time.LocalDateTime;

public record GlobalResponse<T>(boolean success, int status, T data, LocalDateTime timestamp) {
    public static <T> GlobalResponse<T> success(int status, T data) {
        return new GlobalResponse<>(true, status, data, LocalDateTime.now());
    }

    public static <T> GlobalResponse<T> fail(int status, T errorResponse) {
        return new GlobalResponse<>(false, status, errorResponse, LocalDateTime.now());
    }
}
