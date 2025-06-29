package com.lgcns.kafka.message;

import com.fasterxml.jackson.annotation.JsonFormat;
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
                @Schema(description = "예약 날짜", example = "2023-10-01")
                @JsonFormat(
                        shape = JsonFormat.Shape.STRING,
                        pattern = "yyyy-MM-dd",
                        timezone = "Asia/Seoul")
                LocalDate reservationDate,
        @NotNull(message = "예약 시간은 필수입니다.")
                @Schema(description = "팝업 예약 시간", example = "10:00:00")
                @JsonFormat(
                        shape = JsonFormat.Shape.STRING,
                        pattern = "HH:mm:ss",
                        timezone = "Asia/Seoul")
                LocalTime reservationTime) {
    public static MemberEnteredMessage from(QrEntranceInfoRequest qrEntranceInfoRequest) {
        return new MemberEnteredMessage(
                qrEntranceInfoRequest.popupId(),
                qrEntranceInfoRequest.gender(),
                qrEntranceInfoRequest.age(),
                qrEntranceInfoRequest.reservationDate(),
                qrEntranceInfoRequest.reservationTime());
    }
}
