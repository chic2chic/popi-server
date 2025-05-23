package com.lgcns.service;

import com.lgcns.domain.Member;
import com.lgcns.domain.OauthInfo;
import com.lgcns.dto.request.MemberInternalRegisterRequest;
import com.lgcns.dto.request.MemberOauthInfoRequest;
import com.lgcns.dto.response.MemberInfoResponse;
import com.lgcns.dto.response.MemberInternalInfoResponse;
import com.lgcns.dto.response.MemberInternalRegisterResponse;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.MemberErrorCode;
import com.lgcns.repository.MemberRepository;
import java.util.Optional;
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

    @Override
    public MemberInternalRegisterResponse registerMember(MemberInternalRegisterRequest request) {
        if (memberRepository.existsByOauthInfo(
                OauthInfo.createOauthInfo(request.oauthId(), request.oauthProvider()))) {
            throw new CustomException(MemberErrorCode.ALREADY_REGISTERED);
        }

        Member member =
                Member.createMember(
                        OauthInfo.createOauthInfo(request.oauthId(), request.oauthProvider()),
                        request.nickname(),
                        request.gender(),
                        request.age());
        memberRepository.save(member);

        return MemberInternalRegisterResponse.of(member.getId(), member.getRole());
    }

    @Override
    public MemberInternalInfoResponse findOauthInfo(MemberOauthInfoRequest request) {
        Optional<Member> optionalMember =
                memberRepository.findByOauthInfo(
                        OauthInfo.createOauthInfo(request.oauthId(), request.oauthProvider()));

        if (optionalMember.isPresent()) {
            Member member = optionalMember.get();
            return new MemberInternalInfoResponse(
                    member.getId(), member.getRole(), member.getStatus());
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public MemberInternalInfoResponse findMemberId(Long memberId) {
        Member member =
                memberRepository
                        .findById(memberId)
                        .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));

        return new MemberInternalInfoResponse(member.getId(), member.getRole(), member.getStatus());
    }
}
