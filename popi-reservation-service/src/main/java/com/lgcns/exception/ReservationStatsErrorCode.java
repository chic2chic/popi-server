package com.lgcns.exception;

import com.lgcns.error.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReservationStatsErrorCode implements ErrorCode {
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "데이터 베이스 조회 중 에러가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
