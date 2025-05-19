package com.lgcns.service;

import com.lgcns.domain.Member;
import com.lgcns.dto.response.MemberInfoResponse;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.MemberErrorCode;
import com.lgcns.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public MemberInfoResponse findMemberInfo(String memberId) {
        Member member =
                memberRepository
                        .findById(Long.parseLong(memberId))
                        .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));

        return MemberInfoResponse.from(member);
    }
}
