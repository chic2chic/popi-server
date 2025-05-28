package com.lgcns.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MemberReservationStatus {
    PENDING("PENDING"),
    RESERVED("RESERVED"),
    ;

    private final String description;
}
