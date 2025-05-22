package com.lgcns.error.exception;

import lombok.Getter;

@Getter
public class FeignCustomException extends RuntimeException {

    private final int status;
    private final String rawBody;

    public FeignCustomException(int status, String rawBody) {
        this.status = status;
        this.rawBody = rawBody;
    }
}
