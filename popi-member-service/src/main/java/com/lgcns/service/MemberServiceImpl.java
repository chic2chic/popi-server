package com.lgcns.service;

import static com.lgcns.grpc.mapper.MemberGrpcMapper.*;

import com.lgcns.client.AuthServiceClient;
import com.lgcns.domain.Member;
import com.lgcns.domain.OauthInfo;
import com.lgcns.dto.request.MemberOauthInfoRequest;
import com.lgcns.dto.response.MemberInfoResponse;
import com.lgcns.dto.response.MemberInternalInfoResponse;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.MemberErrorCode;
import com.lgcns.repository.MemberRepository;
import com.popi.common.grpc.member.MemberInternalRegisterRequest;
import com.popi.common.grpc.member.MemberInternalRegisterResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final AuthServiceClient authServiceClient;

    @Transactional(readOnly = true)
    public MemberInfoResponse findMemberInfo(String memberId) {
        final Member member = findByMemberId(Long.parseLong(memberId));

        return MemberInfoResponse.from(member);
    }

    @Override
    public void withdrawalMember(String memberId) {
        authServiceClient.deleteRefreshToken(memberId);

        final Member member = findByMemberId(Long.parseLong(memberId));

        member.withdrawal();
    }

    @Override
    public MemberInternalRegisterResponse registerMember(MemberInternalRegisterRequest request) {
        if (memberRepository.existsByOauthInfo(
                OauthInfo.createOauthInfo(request.getOauthId(), request.getOauthProvider()))) {
            throw new CustomException(MemberErrorCode.ALREADY_REGISTERED);
        }

        Member member =
                Member.createMember(
                        OauthInfo.createOauthInfo(request.getOauthId(), request.getOauthProvider()),
                        request.getNickname(),
                        toDomainMemberGender(request.getGender()),
                        toDomainMemberAge(request.getAge()));
        memberRepository.save(member);

        return MemberInternalRegisterResponse.newBuilder()
                .setMemberId(member.getId())
                .setRole(toGrpcMemberRole(member.getRole()))
                .build();
    }

    @Override
    public MemberInternalInfoResponse findOauthInfo(MemberOauthInfoRequest request) {
        Optional<Member> optionalMember =
                memberRepository.findByOauthInfo(
                        OauthInfo.createOauthInfo(request.oauthId(), request.oauthProvider()));

        if (optionalMember.isPresent()) {
            Member member = optionalMember.get();
            return new MemberInternalInfoResponse(
                    member.getId(),
                    member.getNickname(),
                    member.getAge(),
                    member.getGender(),
                    member.getRole(),
                    member.getStatus());
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public MemberInternalInfoResponse findMemberId(Long memberId) {
        final Member member = findByMemberId(memberId);

        return new MemberInternalInfoResponse(
                member.getId(),
                member.getNickname(),
                member.getAge(),
                member.getGender(),
                member.getRole(),
                member.getStatus());
    }

    @Override
    public void rejoinMember(Long memberId) {
        final Member member = findByMemberId(memberId);

        member.reEnroll();
    }

    private Member findByMemberId(Long memberId) {
        return memberRepository
                .findById(memberId)
                .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
