package com.lgcns.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

public record AvailableDateResponse(
        @Schema(description = "팝업 시작 날짜", example = "2025-05-21") LocalDate popupOpenDate,
        @Schema(description = "팝업 종료 날짜", example = "2025-07-22") LocalDate popupCloseDate,
        @Schema(description = "특정 연월에 대한 예약 가능 날짜 조회", implementation = ReservableDate.class)
                List<ReservableDate> reservableDate) {
    public static AvailableDateResponse of(
            LocalDate popupOpenDate,
            LocalDate popupCloseDate,
            List<ReservableDate> reservableDate) {
        return new AvailableDateResponse(popupOpenDate, popupCloseDate, reservableDate);
    }
}
