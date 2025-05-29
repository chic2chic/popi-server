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

    @Lob private String qrImage;

    private LocalDate reservationDate;
    private LocalTime reservationTime;

    @Enumerated(EnumType.STRING)
    private MemberReservationStatus status;

    private Boolean isEntered;

    @Builder
    private MemberReservation(
            Long reservationId,
            Long memberId,
            Long popupId,
            String qrImage,
            LocalDate reservationDate,
            LocalTime reservationTime) {
        this.reservationId = reservationId;
        this.memberId = memberId;
        this.popupId = popupId;
        this.qrImage = qrImage;
        this.reservationDate = reservationDate;
        this.reservationTime = reservationTime;
        this.status = MemberReservationStatus.PENDING;
        this.isEntered = false;
    }

    public static MemberReservation createMemberReservation(Long reservationId, Long memberId) {
        return MemberReservation.builder().reservationId(reservationId).memberId(memberId).build();
    }

    public void updateMemberReservation(
            Long popupId, String qrImage, LocalDate reservationDate, LocalTime reservationTime) {
        this.popupId = popupId;
        this.qrImage = qrImage;
        this.reservationDate = reservationDate;
        this.reservationTime = reservationTime;
        this.status = MemberReservationStatus.RESERVED;
    }

    public void updateIsEntered() {
        this.isEntered = true;
    }
}
