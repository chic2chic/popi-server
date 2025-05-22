package com.lgcns.exception;

import com.lgcns.error.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MemberReservationErrorCode implements ErrorCode {
    INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, "유효한 날짜 범위 또는 yyyy-MM 형식이 아닙니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
