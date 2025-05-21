package com.lgcns.exception;

import com.lgcns.error.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    ID_TOKEN_VERIFICATION_FAILED(HttpStatus.UNAUTHORIZED, "ID 토큰 검증에 실패했습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "유효한 리프레시 토큰이 존재하지 않습니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 리프레시 토큰입니다. 다시 로그인해주세요."),

    EXPIRED_REGISTER_TOKEN(HttpStatus.UNAUTHORIZED, "회원가입 시간이 만료되었습니다. 소셜 로그인을 다시 진행해주세요."),
    ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 가입된 사용자입니다. 로그인 후 이용해주세요."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
