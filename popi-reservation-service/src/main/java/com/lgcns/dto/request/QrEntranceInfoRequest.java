package com.lgcns.dto.request;

import com.lgcns.enums.MemberAge;
import com.lgcns.enums.MemberGender;
import java.time.LocalDate;
import java.time.LocalTime;

public record QrEntranceInfoRequest(
        Long memberReservationId,
        Long reservationId,
        Long popupId,
        MemberAge age,
        MemberGender gender,
        LocalDate reservationDate,
        LocalTime reservationTime) {}
