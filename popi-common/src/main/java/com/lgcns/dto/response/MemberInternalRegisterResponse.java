package com.lgcns.dto.response;

import com.lgcns.enums.MemberRole;

public record MemberInternalRegisterResponse(Long memberId, MemberRole role) {
    public static MemberInternalRegisterResponse of(Long memberId, MemberRole role) {
        return new MemberInternalRegisterResponse(memberId, role);
    }
}
