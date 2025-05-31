package com.lgcns.exception;

import com.lgcns.error.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MemberReservationErrorCode implements ErrorCode {
    RESERVATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 예약했습니다."),
    RESERVATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "예약에 실패했습니다."),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 예약을 찾을 수 없습니다."),

    MEMBER_RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "회원 예약을 찾을 수 없습니다."),

    NO_MORE_RESERVATIONS_AVAILABLE(HttpStatus.BAD_REQUEST, "예약 가능한 수량이 없습니다."),
    QR_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "QR 코드 생성에 실패했습니다."),

    INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, "유효한 날짜 범위 또는 yyyy-MM 형식이 아닙니다."),
    INVALID_SURVEY_CHOICES_COUNT(HttpStatus.BAD_REQUEST, "선택하지 않은 문항이 있습니다."),
    INVALID_QR_CODE(HttpStatus.BAD_REQUEST, "유효하지 않은 QR 코드입니다."),
    RESERVATION_ALREADY_ENTERED(HttpStatus.BAD_REQUEST, "이미 입장한 예약입니다."),
    RESERVATION_POPUP_MISMATCH(HttpStatus.BAD_REQUEST, "예약과 입장 팝업이 일치하지 않습니다."),
    RESERVATION_DATE_MISMATCH(HttpStatus.BAD_REQUEST, "예약 날짜가 일치하지 않습니다."),
    RESERVATION_TIME_MISMATCH(HttpStatus.BAD_REQUEST, "입장 가능 시간이 아닙니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
