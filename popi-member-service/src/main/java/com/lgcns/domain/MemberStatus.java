package com.lgcns.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MemberStatus {
    NORMAL("NORMAL"),
    DELETED("DELETED"),
    FORBIDDEN("FORBIDDEN"),
    ;

    private final String status;
}
