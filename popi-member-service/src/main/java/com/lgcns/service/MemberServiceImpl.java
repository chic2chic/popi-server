package com.lgcns.service;

import static com.lgcns.grpc.mapper.MemberGrpcMapper.*;

import com.lgcns.client.AuthGrpcClient;
import com.lgcns.domain.Member;
import com.lgcns.domain.OauthInfo;
import com.lgcns.dto.response.MemberInfoResponse;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.MemberErrorCode;
import com.lgcns.repository.MemberRepository;
import com.popi.common.grpc.auth.RefreshTokenDeleteRequest;
import com.popi.common.grpc.member.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final AuthGrpcClient authGrpcClient;

    @Transactional(readOnly = true)
    public MemberInfoResponse findMemberInfo(String memberId) {
        final Member member = findByMemberId(Long.parseLong(memberId));

        return MemberInfoResponse.from(member);
    }

    @Override
    public void withdrawalMember(String memberId) {
        authGrpcClient.deleteRefreshToken(
                RefreshTokenDeleteRequest.newBuilder().setMemberId(memberId).build());

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
    public MemberInternalInfoResponse findByOauthInfo(MemberInternalOauthInfoRequest request) {
        Member member =
                memberRepository
                        .findByOauthInfo(
                                OauthInfo.createOauthInfo(
                                        request.getOauthId(), request.getOauthProvider()))
                        .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));

        return MemberInternalInfoResponse.newBuilder()
                .setMemberId(member.getId())
                .setNickname(member.getNickname())
                .setAge(toGrpcMemberAge(member.getAge()))
                .setGender(toGrpcMemberGender(member.getGender()))
                .setRole(toGrpcMemberRole(member.getRole()))
                .setStatus(toGrpcMemberStatus(member.getStatus()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MemberInternalInfoResponse findByMemberId(MemberInternalIdRequest request) {
        final Member member = findByMemberId(request.getMemberId());

        return MemberInternalInfoResponse.newBuilder()
                .setMemberId(member.getId())
                .setNickname(member.getNickname())
                .setAge(toGrpcMemberAge(member.getAge()))
                .setGender(toGrpcMemberGender(member.getGender()))
                .setRole(toGrpcMemberRole(member.getRole()))
                .setStatus(toGrpcMemberStatus(member.getStatus()))
                .build();
    }

    @Override
    public void rejoinMember(MemberInternalIdRequest request) {
        final Member member = findByMemberId(request.getMemberId());

        member.reEnroll();
    }

    private Member findByMemberId(Long memberId) {
        return memberRepository
                .findById(memberId)
                .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
