package com.lgcns.kafka.message;

import com.lgcns.dto.request.QrEntranceInfoRequest;
import com.lgcns.enums.MemberAge;
import com.lgcns.enums.MemberGender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record MemberEnteredMessage(
        @NotNull(message = "팝업 ID는 필수입니다.") @Schema(description = "팝업 ID", example = "1")
                Long popupId,
        @NotNull(message = "방문자 성별은 필수입니다.") @Schema(description = "방문자 성별", example = "FEMALE")
                MemberGender gender,
        @NotNull(message = "방문자 나이대는 필수입니다.") @Schema(description = "방문자 나이대", example = "20")
                MemberAge age,
        @NotNull(message = "예약 날짜는 필수입니다.")
                @Schema(description = "팝업 예약 날짜", example = "2025-05-13")
                LocalDate reservationDate,
        @NotNull(message = "예약 시간은 필수입니다.") @Schema(description = "팝업 예약 시간", example = "10:00:00")
                LocalTime reservationTime) {
    public static MemberEnteredMessage from(QrEntranceInfoRequest qrEntranceInfoRequest) {
        return new MemberEnteredMessage(
                qrEntranceInfoRequest.popupId(),
                qrEntranceInfoRequest.gender(),
                qrEntranceInfoRequest.age(),
                LocalDate.parse(qrEntranceInfoRequest.reservationDate()),
                LocalTime.parse(qrEntranceInfoRequest.reservationTime()));
    }
}
