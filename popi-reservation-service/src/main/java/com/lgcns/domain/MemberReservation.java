package com.lgcns.domain;

import com.lgcns.entity.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberReservation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_reservation_id")
    private Long id;

    private Long reservationId;

    private Long memberId;

    private Long popupId;

    private String imageByte;

    private LocalDate reservationDate;

    private LocalTime reservationTime;

    @Builder
    private MemberReservation(
            Long reservationId,
            Long memberId,
            Long popupId,
            String imageByte,
            LocalDate reservationDate,
            LocalTime reservationTime) {
        this.reservationId = reservationId;
        this.memberId = memberId;
        this.popupId = popupId;
        this.imageByte = imageByte;
        this.reservationDate = reservationDate;
        this.reservationTime = reservationTime;
    }

    public static MemberReservation createMemberReservation(
            Long reservationId,
            Long memberId,
            Long popupId,
            String imageByte,
            LocalDate reservationDate,
            LocalTime reservationTime) {
        return MemberReservation.builder()
                .reservationId(reservationId)
                .memberId(memberId)
                .popupId(popupId)
                .imageByte(imageByte)
                .reservationDate(reservationDate)
                .reservationTime(reservationTime)
                .build();
    }
}
