package com.lgcns.exception;

import com.lgcns.error.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FcmErrorCode implements ErrorCode {
    FCM_FILE_CONVERSION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FCM 키 파일 변환에 실패했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
