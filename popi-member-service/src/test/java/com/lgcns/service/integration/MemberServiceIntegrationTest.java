package com.lgcns.service.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.lgcns.domain.Member;
import com.lgcns.domain.OauthInfo;
import com.lgcns.dto.request.MemberInternalRegisterRequest;
import com.lgcns.dto.request.MemberOauthInfoRequest;
import com.lgcns.dto.response.MemberInfoResponse;
import com.lgcns.dto.response.MemberInternalInfoResponse;
import com.lgcns.dto.response.MemberInternalRegisterResponse;
import com.lgcns.enums.MemberAge;
import com.lgcns.enums.MemberGender;
import com.lgcns.enums.MemberRole;
import com.lgcns.enums.MemberStatus;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.MemberErrorCode;
import com.lgcns.repository.MemberRepository;
import com.lgcns.service.MemberService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MemberServiceIntegrationTest extends WireMockIntegrationTest {

    @Autowired private DatabaseCleaner databaseCleaner;

    @Autowired private MemberService memberService;
    @Autowired private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        databaseCleaner.execute();
    }

    @Nested
    class 회원_정보를_조회할_때 {

        @Test
        void 회원이_존재하면_조회에_성공한다() {
            // given
            registerAuthenticatedMember();

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
            Member member = registerAuthenticatedMember();

            stubFor(
                    delete(urlEqualTo("/internal/1/refresh-token"))
                            .willReturn(aResponse().withStatus(200)));

            // when
            memberService.withdrawalMember(member.getId().toString());

            // then
            member = memberRepository.findById(1L).get();
            assertThat(member.getStatus()).isEqualTo(MemberStatus.DELETED);
        }

        @Test
        void 이미_탈퇴한_회원이_다시_탈퇴하면_예외가_발생한다() {
            // given
            Member member = registerAuthenticatedMember();

            stubFor(
                    delete(urlEqualTo("/internal/1/refresh-token"))
                            .willReturn(aResponse().withStatus(200)));

            memberService.withdrawalMember(member.getId().toString());

            // when & then
            assertThatThrownBy(() -> memberService.withdrawalMember(member.getId().toString()))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(MemberErrorCode.MEMBER_ALREADY_DELETED.getMessage());
        }
    }

    @Nested
    class 인증_서비스의_회원_등록_요청을_처리할_때 {

        @Test
        void 등록되지_않은_회원이면_정상적으로_가입된다() {
            // given
            MemberInternalRegisterRequest request =
                    new MemberInternalRegisterRequest(
                            "testOauthId",
                            "testOauthProvider",
                            "testNickname",
                            MemberAge.TWENTIES,
                            MemberGender.MALE);

            // when
            MemberInternalRegisterResponse response = memberService.registerMember(request);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.memberId()).isEqualTo(1L),
                    () -> assertThat(response.role()).isEqualTo(MemberRole.USER));
        }

        @Test
        void 이미_등록된_회원이면_예외가_발생한다() {
            // given
            registerAuthenticatedMember();
            MemberInternalRegisterRequest request =
                    new MemberInternalRegisterRequest(
                            "testOauthId",
                            "testOauthProvider",
                            "testNickname",
                            MemberAge.TWENTIES,
                            MemberGender.MALE);

            // when & then
            assertThatThrownBy(() -> memberService.registerMember(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(MemberErrorCode.ALREADY_REGISTERED.getMessage());
        }
    }

    @Nested
    class 인증_서비스의_회원_재가입_요청을_처리할_때 {

        @Test
        void 탈퇴한_회원이라면_상태는_NORMAL로_변경된다() {
            // given
            Member member = registerAuthenticatedMember();
            member.withdrawal();

            // when
            memberService.rejoinMember(1L);

            // then
            member = memberRepository.findById(1L).get();
            assertThat(member.getStatus()).isEqualTo(MemberStatus.NORMAL);
        }

        @Test
        void 존재하지_않는_회원이면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> memberService.rejoinMember(999L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }
    }

    @Nested
    class 인증_서비스의_OAuth_회원_조회_요청을_처리할_때 {

        @Test
        void 존재하는_회원이면_회원_정보를_반환한다() {
            // given
            registerAuthenticatedMember();

            MemberOauthInfoRequest request =
                    MemberOauthInfoRequest.of("testOauthId", "testOauthProvider");

            // when
            MemberInternalInfoResponse response = memberService.findOauthInfo(request);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.memberId()).isEqualTo(1L),
                    () -> assertThat(response.nickname()).isEqualTo("testNickname"),
                    () -> assertThat(response.age()).isEqualTo(MemberAge.TWENTIES),
                    () -> assertThat(response.gender()).isEqualTo(MemberGender.MALE),
                    () -> assertThat(response.role()).isEqualTo(MemberRole.USER),
                    () -> assertThat(response.status()).isEqualTo(MemberStatus.NORMAL));
        }

        @Test
        void 존재하지_않는_회원이면_null을_반환한다() {
            // given
            MemberOauthInfoRequest request =
                    MemberOauthInfoRequest.of("nonOauthId", "nonOauthProvider");

            // when
            MemberInternalInfoResponse response = memberService.findOauthInfo(request);

            // then
            assertThat(response).isNull();
        }
    }

    @Nested
    class 인증_서비스의_회원_ID_조회_요청을_처리할_때 {

        @Test
        void 존재하는_회원이면_회원_정보를_반환한다() {
            // given
            registerAuthenticatedMember();

            // when
            MemberInternalInfoResponse response = memberService.findMemberId(1L);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.memberId()).isEqualTo(1L),
                    () -> assertThat(response.role()).isEqualTo(MemberRole.USER),
                    () -> assertThat(response.status()).isEqualTo(MemberStatus.NORMAL));
        }

        @Test
        void 존재하지_않는_회원이면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> memberService.findMemberId(999L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }
    }

    private Member registerAuthenticatedMember() {
        Member member =
                Member.createMember(
                        OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                        "testNickname",
                        MemberGender.MALE,
                        MemberAge.TWENTIES);
        memberRepository.save(member);

        return member;
    }
}
