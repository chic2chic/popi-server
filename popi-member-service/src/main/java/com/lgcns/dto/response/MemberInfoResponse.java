package com.lgcns.dto.response;

import com.lgcns.domain.*;
import com.lgcns.enums.MemberAge;
import com.lgcns.enums.MemberGender;
import com.lgcns.enums.MemberRole;
import com.lgcns.enums.MemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record MemberInfoResponse(
        @Schema(description = "회원 아이디", example = "1") Long memberId,
        @Schema(description = "회원 닉네임", example = "최현태") String nickname,
        @Schema(description = "회원 연령대", example = "20대") MemberAge age,
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
