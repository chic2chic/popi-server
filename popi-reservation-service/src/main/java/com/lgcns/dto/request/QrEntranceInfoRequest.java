package com.lgcns.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lgcns.enums.MemberAge;
import com.lgcns.enums.MemberGender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record QrEntranceInfoRequest(
        @Schema(description = "회원 예약 ID", example = "1") @NotNull(message = "회원 예약 ID는 필수입니다.")
                Long memberReservationId,
        @Schema(description = "예약 ID", example = "1") @NotNull(message = "예약 ID는 필수입니다.")
                Long reservationId,
        @Schema(description = "팝업 ID", example = "1") @NotNull(message = "팝업 ID는 필수입니다.")
                Long popupId,
        @Schema(description = "연령대", example = "TWENTIES") @NotNull(message = "연령대는 비워둘 수 없습니다.")
                MemberAge age,
        @Schema(description = "성별", example = "MALE") @NotNull(message = "성별은 비워둘 수 없습니다.")
                MemberGender gender,
        @Schema(description = "예약 날짜", example = "2025-05-13")
                @NotNull(message = "예약 날짜는 필수입니다.")
                @JsonFormat(
                        shape = JsonFormat.Shape.STRING,
                        pattern = "yyyy-MM-dd",
                        timezone = "Asia/Seoul")
                LocalDate reservationDate,
        @Schema(description = "예약 시간", example = "10:00:00")
                @NotNull(message = "예약 시간은 필수입니다.")
                @JsonFormat(
                        shape = JsonFormat.Shape.STRING,
                        pattern = "HH:mm:ss",
                        timezone = "Asia/Seoul")
                LocalTime reservationTime) {}
