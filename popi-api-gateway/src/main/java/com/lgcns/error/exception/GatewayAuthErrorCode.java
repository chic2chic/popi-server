package com.lgcns.error.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GatewayAuthErrorCode {
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 액세스 토큰입니다. 올바른 토큰으로 요청해주세요."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
