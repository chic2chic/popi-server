package com.lgcns.domain;

import com.lgcns.entity.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
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

    private Long memberId;

    private Long popupId;

    private String imageByte;

    private LocalDate reservationDate;

    private LocalTime reservationTime;
}
