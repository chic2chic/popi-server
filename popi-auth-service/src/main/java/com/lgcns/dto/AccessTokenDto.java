package com.lgcns.dto;

import com.lgcns.domain.MemberRole;

public record AccessTokenDto(Long memberId, MemberRole role, String accessTokenValue) {
    public static AccessTokenDto of(Long memberId, MemberRole role, String accessTokenValue) {
        return new AccessTokenDto(memberId, role, accessTokenValue);
    }
}
