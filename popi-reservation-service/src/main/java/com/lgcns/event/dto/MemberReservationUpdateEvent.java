package com.lgcns.event.dto;

public record MemberReservationUpdateEvent(Long memberReservationId, Long waitTime) {
    public static MemberReservationUpdateEvent of(Long memberReservationId, Long waitTime) {
        return new MemberReservationUpdateEvent(memberReservationId, waitTime);
    }
}
