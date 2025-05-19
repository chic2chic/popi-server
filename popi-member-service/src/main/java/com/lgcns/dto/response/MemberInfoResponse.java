package com.lgcns.dto.response;

import com.lgcns.domain.Member;
import com.lgcns.domain.MemberGender;
import com.lgcns.domain.MemberRole;
import com.lgcns.domain.MemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record MemberInfoResponse(
        @Schema(description = "회원 아이디", example = "1") Long memberId,
        @Schema(description = "회원 닉네임", example = "최현태") String nickname,
        @Schema(description = "회원 나이", example = "20") Integer age,
        @Schema(description = "회원 성별", example = "MALE") MemberGender gender,
        @Schema(description = "회원 상태", example = "NORMAL") MemberStatus status,
        @Schema(description = "회원 역할", example = "ROLE_USER") MemberRole role) {
    public static MemberInfoResponse from(Member member) {
        return new MemberInfoResponse(
                member.getId(),
                member.getNickname(),
                member.getAge(),
                member.getGender(),
                member.getStatus(),
                member.getRole());
    }
}
