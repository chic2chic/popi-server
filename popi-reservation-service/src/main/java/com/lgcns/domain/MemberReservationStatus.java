package com.lgcns.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MemberReservationStatus {
    PENDING("PENDING"),
    CANCELED("CANCELED"),
    RESERVED("RESERVED"),
    ;

    private final String description;
}
