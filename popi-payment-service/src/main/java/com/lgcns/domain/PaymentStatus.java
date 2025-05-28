package com.lgcns.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentStatus {
    READY("READY"),
    PAID("PAID"),
    FAILED("FAILED"),
    CANCELLED("CANCELLED"),
    ;

    private final String status;
}
