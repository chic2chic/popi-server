package com.lgcns.exception;

import com.lgcns.error.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PaymentErrorCode implements ErrorCode {
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 대상 상품 정보를 찾을 수 없습니다."),
    OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "상품 재고가 부족합니다."),

    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다."),
    NOT_PAID(HttpStatus.BAD_REQUEST, "결제가 완료되지 않았습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
