package com.lgcns.exception;

import com.lgcns.error.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MemberErrorCode implements ErrorCode {
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),

    ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 가입된 사용자입니다. 로그인 후 이용해주세요."),

    MEMBER_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 탈퇴한 회원입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
