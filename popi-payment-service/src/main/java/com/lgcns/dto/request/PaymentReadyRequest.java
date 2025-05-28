package com.lgcns.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PaymentReadyRequest(
        @NotNull(message = "팝업 ID는 비워둘 수 없습니다.") @Schema(description = "팝업 ID", example = "1")
                Long popupId,
        @NotNull(message = "상품 목록은 비워둘 수 없습니다.") @Schema(description = "결제할 상품 목록")
                List<Item> items) {
    @Schema(description = "결제할 개별 상품 정보")
    public record Item(
            @NotNull(message = "상품 ID는 비워둘 수 없습니다.") @Schema(description = "상품 ID", example = "11")
                    Long itemId,
            @NotNull(message = "구매 수량은 비워둘 수 없습니다.") @Schema(description = "구매 수량", example = "2")
                    Integer quantity) {}
}
