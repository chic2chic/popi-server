package com.lgcns.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ItemBuyerCountResponse(
        @Schema(description = "상품 ID", example = "1") Long itemId,
        @Schema(description = "구매자 수", example = "42") long buyerCount) {}
