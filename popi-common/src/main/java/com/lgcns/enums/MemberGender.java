package com.lgcns.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MemberGender {
    MALE("MALE"),
    FEMALE("FEMALE"),
    ;

    private final String gender;
}
