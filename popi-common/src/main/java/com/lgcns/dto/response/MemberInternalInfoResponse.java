package com.lgcns.dto.response;

import com.lgcns.enums.MemberAge;
import com.lgcns.enums.MemberGender;
import com.lgcns.enums.MemberRole;
import com.lgcns.enums.MemberStatus;

public record MemberInternalInfoResponse(
        Long memberId,
        String nickname,
        MemberAge age,
        MemberGender gender,
        MemberRole role,
        MemberStatus status) {}
