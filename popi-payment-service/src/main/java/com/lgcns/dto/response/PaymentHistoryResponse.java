package com.lgcns.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentHistoryResponse(
        @Schema(description = "결제 ID", example = "1") Long paymentId,
        @Schema(description = "팝업 ID", example = "1") Long popupId,
        @Schema(description = "결제 완료 일시", example = "2024-05-31T14:00:00") LocalDateTime paidAt,
        @Schema(description = "결제에 포함된 상품 목록") List<Item> items) {
    public record Item(
            @Schema(description = "상품 이름", example = "DAZED 지수") String itemName,
            @Schema(description = "구매 수량", example = "1") int quantity,
            @Schema(description = "상품 결제 금액", example = "15000") int price) {}
}
