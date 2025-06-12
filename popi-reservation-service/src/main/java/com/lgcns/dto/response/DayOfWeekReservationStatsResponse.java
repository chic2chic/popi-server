package com.lgcns.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record DayOfWeekReservationStatsResponse(
        @Schema(description = "팝업 ID", example = "1") Long popupId,
        @Schema(description = "월요일 예약 수", example = "10") int mondayCount,
        @Schema(description = "화요일 예약 수", example = "12") int tuesdayCount,
        @Schema(description = "수요일 예약 수", example = "8") int wednesdayCount,
        @Schema(description = "목요일 예약 수", example = "15") int thursdayCount,
        @Schema(description = "금요일 예약 수", example = "20") int fridayCount,
        @Schema(description = "토요일 예약 수", example = "25") int saturdayCount,
        @Schema(description = "일요일 예약 수", example = "18") int sundayCount) {
    public static DayOfWeekReservationStatsResponse of(
            Long popupId,
            int mondayCount,
            int tuesdayCount,
            int wednesdayCount,
            int thursdayCount,
            int fridayCount,
            int saturdayCount,
            int sundayCount) {
        return new DayOfWeekReservationStatsResponse(
                popupId,
                mondayCount,
                tuesdayCount,
                wednesdayCount,
                thursdayCount,
                fridayCount,
                saturdayCount,
                sundayCount);
    }
}
