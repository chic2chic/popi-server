package com.lgcns.dto.response;

public record PopupMapResponse(
        Long popupId,
        String popupName,
        String imageUrl,
        String popupOpenDate,
        String popupCloseDate,
        String address,
        Double latitude,
        Double longitude) {
    public static PopupMapResponse of(
            Long popupId,
            String popupName,
            String imageUrl,
            String popupOpenDate,
            String popupCloseDate,
            String address,
            Double latitude,
            Double longitude) {
        return new PopupMapResponse(
                popupId,
                popupName,
                imageUrl,
                popupOpenDate,
                popupCloseDate,
                address,
                latitude,
                longitude);
    }
}
