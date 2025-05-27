package com.lgcns.client.managerClient.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ReservationPopupInfoResponse(
        @Schema(description = "팝업스토어 ID", example = "1") Long popupId,
        @Schema(description = "팝업스토어 이름", example = "BLACKPINK 팝업스토어") String popupName,
        @Schema(description = "팝업스토어 주소", example = "서울특별시 강남구 테헤란로 12, 1층 201호") String address,
        @Schema(description = "위도", example = "37.5665") Double latitude,
        @Schema(description = "경도", example = "126.9780") Double longitude) {
    public static ReservationPopupInfoResponse of(
            Long popupId, String popupName, String address, Double latitude, Double longitude) {
        return new ReservationPopupInfoResponse(popupId, popupName, address, latitude, longitude);
    }
}
