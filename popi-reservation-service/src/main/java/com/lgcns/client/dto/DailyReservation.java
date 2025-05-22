package com.lgcns.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

public record DailyReservation(
        @Schema(description = "예약 날짜", example = "2025-05-21") LocalDate reservationDate,
        @Schema(description = "예약 번호와 시간", implementation = TimeSlot.class)
                List<TimeSlot> timeSlots) {}
