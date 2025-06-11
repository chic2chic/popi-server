package com.lgcns.event.dto;

import com.lgcns.domain.MemberReservation;
import java.time.LocalDate;
import java.time.LocalTime;

public record MemberReservationNotificationEvent(
        Long memberReservationId,
        Long memberId,
        LocalDate reservationDate,
        LocalTime reservationTime) {
    public static MemberReservationNotificationEvent from(MemberReservation memberReservation) {
        return new MemberReservationNotificationEvent(
                memberReservation.getId(),
                memberReservation.getMemberId(),
                memberReservation.getReservationDate(),
                memberReservation.getReservationTime());
    }
}
