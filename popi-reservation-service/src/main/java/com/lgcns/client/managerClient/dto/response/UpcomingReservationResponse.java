package com.lgcns.client.managerClient.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record UpcomingReservationResponse(
        @Schema(description = "회원 아이디", example = "1") Long memberId,
        @Schema(description = "예약 날짜", example = "2025-05-27") LocalDate reservationDate) {
    public static UpcomingReservationResponse of(Long memberId, LocalDate reservationDate) {
        return new UpcomingReservationResponse(memberId, reservationDate);
    }
}
