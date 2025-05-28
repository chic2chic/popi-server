package com.lgcns.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record PaymentReadyResponse(
        @Schema(description = "구매자 이름", example = "최현태") String buyerName,
        @Schema(description = "결제 항목 이름 (대표 상품명 외 N건)", example = "피규어 제니 외 1건") String name,
        @Schema(description = "결제 총 금액", example = "206000") int amount,
        @Schema(
                        description = "결제 요청 고유 ID (merchant_uid)",
                        example = "popup_1_order_6ef377d3-e92c-46e6-b38a-eb87227da444")
                String merchantUid) {
    public static PaymentReadyResponse of(
            String buyerName, String name, int amount, String merchantUid) {
        return new PaymentReadyResponse(buyerName, name, amount, merchantUid);
    }
}
