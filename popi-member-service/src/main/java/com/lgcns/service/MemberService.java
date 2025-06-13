package com.lgcns.service;

import com.lgcns.dto.request.MemberOauthInfoRequest;
import com.lgcns.dto.response.MemberInfoResponse;
import com.lgcns.dto.response.MemberInternalInfoResponse;
import com.popi.common.grpc.member.MemberInternalRegisterRequest;
import com.popi.common.grpc.member.MemberInternalRegisterResponse;

public interface MemberService {
    MemberInfoResponse findMemberInfo(String memberId);

    void withdrawalMember(String memberId);

    MemberInternalRegisterResponse registerMember(MemberInternalRegisterRequest request);

    MemberInternalInfoResponse findOauthInfo(MemberOauthInfoRequest request);

    MemberInternalInfoResponse findMemberId(Long memberId);

    void rejoinMember(Long memberId);
}
