package com.lgcns.error;

import com.lgcns.error.exception.CustomException;
import com.lgcns.error.exception.ErrorCode;
import com.lgcns.response.GlobalResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice(basePackages = "com.lgcns")
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<GlobalResponse> handleCustomException(CustomException e) {
        final ErrorCode errorCode = e.getErrorCode();
        final ErrorResponse errorResponse =
                ErrorResponse.of(errorCode.getErrorName(), errorCode.getMessage());
        final GlobalResponse response =
                GlobalResponse.fail(errorCode.getHttpStatus().value(), errorResponse);

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }
}
