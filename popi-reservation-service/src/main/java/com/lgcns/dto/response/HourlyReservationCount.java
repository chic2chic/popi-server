package com.lgcns.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

public record HourlyReservationCount(
        @Schema(description = "예약 시간", example = "10:00") LocalTime reservationTime,
        @Schema(description = "해당 시간 예약자 수", example = "12") Integer count) {}
