package com.lgcns.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

public record MonthlyReservationDto(
        @Schema(description = "팝업 시작 날짜", example = "2025-05-21") LocalDate popupOpenDate,
        @Schema(description = "팝업 종료 날짜", example = "2025-06-21") LocalDate popupCloseDate,
        @Schema(description = "시간별 수용 인원수", example = "200") Integer timeCapacity,
        @Schema(description = "날짜별 예약 번호와 시간", implementation = DailyReservation.class)
                List<DailyReservation> dailyReservations) {}
