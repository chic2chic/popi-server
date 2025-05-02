package com.lgcns.error.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GatewayAuthErrorCode {
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않습니다. 올바른 토큰으로 요청해주세요."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
