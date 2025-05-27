package com.lgcns.dto.response;

import com.lgcns.client.managerClient.dto.response.ReservationPopupInfoResponse;
import com.lgcns.domain.MemberReservation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.TextStyle;
import java.util.Locale;

public record ReservationInfoResponse(
        @Schema(description = "예약 ID", example = "23") Long reservationId,
        @Schema(description = "팝업 ID", example = "1") Long popupId,
        @Schema(description = "팝업 이름", example = "블랙핑크 팝업스토어") String popupName,
        @Schema(description = "예약 날짜", example = "2025-05-26") String reservationDate,
        @Schema(description = "예약 시간", example = "11:00") String reservationTime,
        @Schema(description = "예약 요일", example = "MON") String reservationDay,
        @Schema(description = "주소", example = "서울 영등포구 여의대로 108 더현대서울") String address,
        @Schema(description = "위도", example = "37.1234561234567") Double latitude,
        @Schema(description = "경도", example = "127.37123456123456") Double longitude,
        @Schema(description = "QR 이미지 (Base64 인코딩)", example = "iVBORw0KGgoAAAA...")
                String qrImage) {
    public static ReservationInfoResponse of(
            MemberReservation reservation, ReservationPopupInfoResponse reservationPopupInfo) {
        return new ReservationInfoResponse(
                reservation.getId(),
                reservationPopupInfo.popupId(),
                reservationPopupInfo.popupName(),
                reservation.getReservationDate().toString(),
                reservation.getReservationTime().toString(),
                reservation
                        .getReservationDate()
                        .getDayOfWeek()
                        .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                        .toUpperCase(),
                reservationPopupInfo.address(),
                reservationPopupInfo.latitude(),
                reservationPopupInfo.longitude(),
                reservation.getQrImage());
    }
}
