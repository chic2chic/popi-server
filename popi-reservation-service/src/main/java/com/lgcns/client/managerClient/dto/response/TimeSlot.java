package com.lgcns.client.managerClient.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

public record TimeSlot(
        @Schema(description = "예약 번호", example = "4") Long reservationId,
        @Schema(description = "예약 시간", example = "10:00:00") LocalTime time) {}
