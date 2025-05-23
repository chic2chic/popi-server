package com.lgcns.service;

import com.lgcns.dto.request.MemberInternalRegisterRequest;
import com.lgcns.dto.response.MemberInfoResponse;
import com.lgcns.dto.response.MemberInternalRegisterResponse;

public interface MemberService {
    MemberInfoResponse findMemberInfo(String memberId);

    MemberInternalRegisterResponse registerMember(MemberInternalRegisterRequest request);
}
