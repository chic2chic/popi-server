package com.lgcns.dto.request;

import com.lgcns.enums.MemberAge;
import com.lgcns.enums.MemberGender;

public record MemberInternalRegisterRequest(
        String oauthId,
        String oauthProvider,
        String nickname,
        MemberAge age,
        MemberGender gender) {}
