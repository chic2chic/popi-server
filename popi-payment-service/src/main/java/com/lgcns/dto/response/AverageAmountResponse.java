package com.lgcns.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AverageAmountResponse(
        @Schema(description = "전체 기간 1인 평균 구매액", example = "30000") Integer totalAverageAmount,
        @Schema(description = "오늘 1인 평균 구매액", example = "5000") Integer todayAverageAmount) {
    public static AverageAmountResponse of(Integer totalAverageAmount, Integer todayAverageAmount) {
        return new AverageAmountResponse(totalAverageAmount, todayAverageAmount);
    }
}
