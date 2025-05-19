package com.lgcns.service;

import com.lgcns.dto.response.MemberInfoResponse;

public interface MemberService {
    MemberInfoResponse findMemberInfo(String memberId);
}
