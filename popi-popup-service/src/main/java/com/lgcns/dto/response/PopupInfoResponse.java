package com.lgcns.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record PopupInfoResponse(
        @Schema(description = "팝업스토어 ID", example = "1") Long popupId,
        @Schema(description = "팝업스토어 이름", example = "BLACKPINK 팝업스토어") String popupName,
        @Schema(description = "팝업스토어 이미지 URL", example = "https://bucket/asdf") String imageUrl,
        @Schema(description = "팝업스토어 오픈 날짜", example = "2025-05-05") String popupOpenDate,
        @Schema(description = "팝업스토어 마감 날짜", example = "2025-06-06") String popupCloseDate,
        @Schema(description = "팝업스토어 주소", example = "서울특별시 강남구 테헤란로 12, 1층 201호") String address) {
    public static PopupInfoResponse of(
            Long popupId,
            String popupName,
            String imageUrl,
            String popupOpenDate,
            String popupCloseDate,
            String address) {
        return new PopupInfoResponse(
                popupId, popupName, imageUrl, popupOpenDate, popupCloseDate, address);
    }
}
