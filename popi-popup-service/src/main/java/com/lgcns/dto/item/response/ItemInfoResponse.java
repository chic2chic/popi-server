package com.lgcns.dto.item.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ItemInfoResponse(
        @Schema(description = "상품 ID", example = "1") Long itemId,
        @Schema(description = "상품명", example = "지수 포토카드") String name,
        @Schema(description = "상품 이미지 URL", example = "https://bucket/asdf") String imageUrl,
        @Schema(description = "상품 가격", example = "50000") int price) {
    public static ItemInfoResponse of(Long itemId, String name, String imageUrl, int price) {
        return new ItemInfoResponse(itemId, name, imageUrl, price);
    }
}
