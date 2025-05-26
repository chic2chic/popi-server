package com.lgcns.dto.popup.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record PopupDetailsResponse(
        @Schema(description = "팝업스토어 ID", example = "1") Long popupId,
        @Schema(description = "팝업스토어 이름", example = "BLACKPINK 팝업스토어") String popupName,
        @Schema(description = "팝업스토어 이미지 URL", example = "https://bucket/asdf") String imageUrl,
        @Schema(description = "팝업스토어 오픈 날짜", example = "2025-05-05") String popupOpenDate,
        @Schema(description = "팝업스토어 마감 날짜", example = "2025-06-06") String popupCloseDate,
        @Schema(description = "팝업스토어 예약 시작 일시", example = "2025-07-27 10:00:00")
                String reservationOpenDateTime,
        @Schema(description = "팝업스토어 예약 마감 일시", example = "2025-08-28 20:00:00")
                String reservationCloseDateTime,
        @Schema(description = "팝업스토어 주소", example = "서울특별시 강남구 테헤란로 12, 1층 201호") String address,
        @Schema(description = "팝업 매장 오픈 시간", example = "10:00:00") String runOpenTime,
        @Schema(description = "팝업 매장 마감 시간", example = "20:00:00") String runCloseTime,
        @Schema(description = "위도", example = "37.123456") Double latitude,
        @Schema(description = "경도", example = "127.123456") Double longitude) {
    public static PopupDetailsResponse of(
            Long popupId,
            String popupName,
            String imageUrl,
            String popupOpenDate,
            String popupCloseDate,
            String reservationOpenDateTime,
            String reservationCloseDateTime,
            String address,
            String runOpenTime,
            String runCloseTime,
            Double latitude,
            Double longitude) {
        return new PopupDetailsResponse(
                popupId,
                popupName,
                imageUrl,
                popupOpenDate,
                popupCloseDate,
                reservationOpenDateTime,
                reservationCloseDateTime,
                address,
                runOpenTime,
                runCloseTime,
                latitude,
                longitude);
    }
}
