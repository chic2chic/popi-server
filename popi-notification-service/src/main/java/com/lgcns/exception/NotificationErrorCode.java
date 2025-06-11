package com.lgcns.exception;

import com.lgcns.error.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements ErrorCode {
    FCM_FILE_CONVERSION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FCM 키 파일 변환에 실패했습니다."),
    FCM_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FCM 메세지 전송에 실패했습니다"),
    FCM_TOKEN_DUPLICATED(HttpStatus.BAD_REQUEST, "중복된 FCM 토큰이 존재합니다."),

    REDIS_ACCESS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Redis 접근에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
