package com.lgcns.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

public record ReservableTime(
        @Schema(description = "팝업 예약 번호", example = "12") Long reservationId,
        @Schema(description = "팝업 예약 시간", example = "10:00:00") LocalTime time,
        @Schema(description = "해당 날짜, 시간에 대한 예약 가능 여부", example = "true") Boolean isPossible) {}
