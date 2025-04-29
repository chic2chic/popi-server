package com.lgcns.error.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    HttpStatus getHttpStatus();

    String getMessage();

    default String getErrorName() {
        return this.toString();
    }
}
