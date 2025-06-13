package com.lgcns.service;

import com.lgcns.dto.response.MemberInfoResponse;
import com.popi.common.grpc.member.*;

public interface MemberService {
    MemberInfoResponse findMemberInfo(String memberId);

    void withdrawalMember(String memberId);

    MemberInternalRegisterResponse registerMember(MemberInternalRegisterRequest request);

    MemberInternalInfoResponse findByOauthInfo(MemberInternalOauthInfoRequest request);

    com.lgcns.dto.response.MemberInternalInfoResponse findMemberId(Long memberId);

    void rejoinMember(Long memberId);
}
