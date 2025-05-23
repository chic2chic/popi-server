package com.lgcns.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

public record ReservableDate(
        @Schema(description = "특정 날짜", example = "2025-05-23") LocalDate date,
        @Schema(description = "특정 날짜에 대한 예약 가능 여부", example = "true") Boolean isReservable,
        @Schema(description = "팝업 시작 날짜", implementation = ReservableTime.class)
                List<ReservableTime> timeSlots) {
    public static ReservableDate of(
            LocalDate date, Boolean isReservable, List<ReservableTime> timeSlots) {
        return new ReservableDate(date, isReservable, timeSlots);
    }
}
