package com.lgcns.service.unit;

import static com.lgcns.grpc.mapper.MemberGrpcMapper.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;

import com.lgcns.client.AuthGrpcClient;
import com.lgcns.domain.Member;
import com.lgcns.domain.OauthInfo;
import com.lgcns.dto.response.MemberInfoResponse;
import com.lgcns.enums.MemberAge;
import com.lgcns.enums.MemberGender;
import com.lgcns.enums.MemberRole;
import com.lgcns.enums.MemberStatus;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.MemberErrorCode;
import com.lgcns.repository.MemberRepository;
import com.lgcns.service.MemberServiceImpl;
import com.popi.common.grpc.auth.RefreshTokenDeleteRequest;
import com.popi.common.grpc.member.*;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberServiceUnitTest {

    @InjectMocks private MemberServiceImpl memberService;
    @Mock private MemberRepository memberRepository;

    @Mock private AuthGrpcClient authGrpcClient;

    @Nested
    class 회원_정보를_조회할_때 {

        @Test
        void 회원이_존재하면_조회에_성공한다() {
            // given
            Member member = createTestMember(1L, MemberStatus.NORMAL);
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));

            // when
            MemberInfoResponse response = memberService.findMemberInfo("1");

            // then
            Assertions.assertAll(
                    () -> assertThat(response.memberId()).isEqualTo(1L),
                    () -> assertThat(response.nickname()).isEqualTo("testNickname"),
                    () -> assertThat(response.age()).isEqualTo(MemberAge.TWENTIES),
                    () -> assertThat(response.gender()).isEqualTo(MemberGender.MALE),
                    () -> assertThat(response.status()).isEqualTo(MemberStatus.NORMAL),
                    () -> assertThat(response.role()).isEqualTo(MemberRole.USER));
        }

        @Test
        void 회원이_존재하지_않으면_예외가_발생한다() {
            // given
            given(memberRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.findMemberInfo("1"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }
    }

    @Nested
    class 회원_탈퇴할_때 {

        @Test
        void 회원이_탈퇴하면_상태는_DELETED가_된다() {
            // given
            Member member = createTestMember(1L, MemberStatus.NORMAL);
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));

            // when
            memberService.withdrawalMember(member.getId().toString());

            // then
            verify(authGrpcClient).deleteRefreshToken(any(RefreshTokenDeleteRequest.class));
            assertThat(member.getStatus()).isEqualTo(MemberStatus.DELETED);
        }

        @Test
        void 이미_탈퇴한_회원이_다시_탈퇴하면_예외가_발생한다() {
            // given
            Member member = createTestMember(1L, MemberStatus.DELETED);
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));

            // when & then
            assertThatThrownBy(() -> memberService.withdrawalMember(member.getId().toString()))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(MemberErrorCode.MEMBER_ALREADY_DELETED.getMessage());
        }
    }

    @Nested
    class 인증_서비스의_회원_등록_요청을_처리할_때 {

        MemberInternalRegisterRequest grpcRequest =
                MemberInternalRegisterRequest.newBuilder()
                        .setOauthId("testOauthId")
                        .setOauthProvider("testOauthProvider")
                        .setNickname("testNickname")
                        .setAge(toGrpcMemberAge(MemberAge.TWENTIES))
                        .setGender(toGrpcMemberGender(MemberGender.MALE))
                        .build();

        @Test
        void 등록되지_않은_회원이면_정상적으로_가입된다() {
            // given
            given(memberRepository.existsByOauthInfo(any(OauthInfo.class))).willReturn(false);
            given(memberRepository.save(any(Member.class)))
                    .willAnswer(
                            invocation -> {
                                Member saved = invocation.getArgument(0);
                                ReflectionTestUtils.setField(saved, "id", 1L);
                                return saved;
                            });

            // when
            MemberInternalRegisterResponse grpcResponse = memberService.registerMember(grpcRequest);

            // then
            Assertions.assertAll(
                    () -> assertThat(grpcResponse.getMemberId()).isEqualTo(1L),
                    () ->
                            assertThat(grpcResponse.getRole())
                                    .isEqualTo(toGrpcMemberRole(MemberRole.USER)));
        }

        @Test
        void 이미_등록된_회원이면_예외가_발생한다() {
            // given
            given(memberRepository.existsByOauthInfo(any(OauthInfo.class))).willReturn(true);

            // when & then
            assertThatThrownBy(() -> memberService.registerMember(grpcRequest))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(MemberErrorCode.ALREADY_REGISTERED.getMessage());
        }
    }

    @Nested
    class 인증_서비스의_회원_재가입_요청을_처리할_때 {

        @Test
        void 탈퇴한_회원이라면_상태는_NORMAL로_변경된다() {
            // given
            MemberInternalIdRequest grpcRequest =
                    MemberInternalIdRequest.newBuilder().setMemberId(1L).build();

            Member member = createTestMember(1L, MemberStatus.DELETED);
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));

            // when
            memberService.rejoinMember(grpcRequest);

            // then
            assertThat(member.getStatus()).isEqualTo(MemberStatus.NORMAL);
        }

        @Test
        void 존재하지_않는_회원이면_예외가_발생한다() {
            // given
            MemberInternalIdRequest grpcRequest =
                    MemberInternalIdRequest.newBuilder().setMemberId(999L).build();

            // when & then
            assertThatThrownBy(() -> memberService.rejoinMember(grpcRequest))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }
    }

    @Nested
    class 인증_서비스의_OAuth_회원_조회_요청을_처리할_때 {

        MemberInternalOauthInfoRequest grpcRequest =
                MemberInternalOauthInfoRequest.newBuilder()
                        .setOauthId("testOauthId")
                        .setOauthProvider("testOauthProvider")
                        .build();

        @Test
        void 존재하는_회원이면_회원_정보를_반환한다() {
            // given
            Member member = createTestMember(1L, MemberStatus.NORMAL);
            given(memberRepository.findByOauthInfo(any(OauthInfo.class)))
                    .willReturn(Optional.of(member));

            // when
            MemberInternalInfoResponse grpcResponse = memberService.findByOauthInfo(grpcRequest);

            // then
            Assertions.assertAll(
                    () -> assertThat(grpcResponse.getMemberId()).isEqualTo(1L),
                    () -> assertThat(grpcResponse.getNickname()).isEqualTo("testNickname"),
                    () ->
                            assertThat(grpcResponse.getAge())
                                    .isEqualTo(toGrpcMemberAge(MemberAge.TWENTIES)),
                    () ->
                            assertThat(grpcResponse.getGender())
                                    .isEqualTo(toGrpcMemberGender(MemberGender.MALE)),
                    () ->
                            assertThat(grpcResponse.getRole())
                                    .isEqualTo(toGrpcMemberRole(MemberRole.USER)),
                    () ->
                            assertThat(grpcResponse.getStatus())
                                    .isEqualTo(toGrpcMemberStatus(MemberStatus.NORMAL)));
        }

        @Test
        void 존재하지_않는_회원이면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> memberService.findByOauthInfo(grpcRequest))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }
    }

    @Nested
    class 인증_서비스의_회원_ID_조회_요청을_처리할_때 {

        @Test
        void 존재하는_회원이면_회원_정보를_반환한다() {
            // given
            MemberInternalIdRequest grpcRequest =
                    MemberInternalIdRequest.newBuilder().setMemberId(1L).build();

            Member member = createTestMember(1L, MemberStatus.NORMAL);
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));

            // when
            MemberInternalInfoResponse grpcResponse = memberService.findByMemberId(grpcRequest);

            // then
            Assertions.assertAll(
                    () -> assertThat(grpcResponse.getMemberId()).isEqualTo(1L),
                    () ->
                            assertThat(grpcResponse.getRole())
                                    .isEqualTo(toGrpcMemberRole(MemberRole.USER)),
                    () ->
                            assertThat(grpcResponse.getStatus())
                                    .isEqualTo(toGrpcMemberStatus(MemberStatus.NORMAL)));
        }

        @Test
        void 존재하지_않는_회원이면_예외가_발생한다() {
            // given
            MemberInternalIdRequest grpcRequest =
                    MemberInternalIdRequest.newBuilder().setMemberId(999L).build();

            // when & then
            assertThatThrownBy(() -> memberService.findByMemberId(grpcRequest))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }
    }

    private Member createTestMember(Long id, MemberStatus status) {
        Member member =
                Member.createMember(
                        OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                        "testNickname",
                        MemberGender.MALE,
                        MemberAge.TWENTIES);

        ReflectionTestUtils.setField(member, "id", id);
        ReflectionTestUtils.setField(member, "status", status);

        return member;
    }
}
