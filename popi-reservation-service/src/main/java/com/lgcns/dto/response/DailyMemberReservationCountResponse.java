package com.lgcns.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record DailyMemberReservationCountResponse(
        @Schema(description = "오늘 예약자 수", example = "100") Long reservationCount) {
    public static DailyMemberReservationCountResponse of(Long reservationCount) {
        return new DailyMemberReservationCountResponse(reservationCount);
    }
}
