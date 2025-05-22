package com.lgcns.error.feign;

import com.lgcns.error.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public record FeignErrorCode(String errorName, String message, int statusCode)
        implements ErrorCode {

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.resolve(statusCode) != null
                ? HttpStatus.resolve(statusCode)
                : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getErrorName() {
        return errorName;
    }
}
