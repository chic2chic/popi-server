package com.lgcns.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

public record DailyReservationCountResponse(
        @Schema(description = "예약 날짜", example = "2025-05-11") LocalDate reservationDate,
        @Schema(description = "시간별 예약자 수 목록")
                List<HourlyReservationCount> hourlyReservationCountList) {}
