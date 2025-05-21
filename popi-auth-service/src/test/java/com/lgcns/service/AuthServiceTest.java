package com.lgcns.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.lgcns.domain.*;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.MemberErrorCode;
import com.lgcns.repository.MemberRepository;
import com.lgcns.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

@SpringBootTest
public class AuthServiceTest {

    @Autowired private AuthService memberService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    private Member registerAuthenticatedMember() {
        Member member =
                Member.createMember(
                        OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                        "testNickname",
                        MemberGender.MALE,
                        MemberAge.TWENTIES);
        memberRepository.save(member);

        UserDetails userDetails =
                User.withUsername(member.getId().toString())
                        .password("")
                        .authorities(member.getRole().toString())
                        .build();
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(token);

        return member;
    }

    @Nested
    class 로그아웃_시 {
        @Test
        void 로그아웃하면_리프레시_토큰이_삭제된다() {
            // given
            Member member = registerAuthenticatedMember();

            RefreshToken refreshToken =
                    RefreshToken.builder()
                            .memberId(member.getId())
                            .token("testRefreshToken")
                            .build();
            refreshTokenRepository.save(refreshToken);

            // when
            memberService.logoutMember(member.getId().toString());

            // then
            assertThat(refreshTokenRepository.findById(member.getId())).isEmpty();
        }
    }

    @Nested
    class 회원_탈퇴_시 {
        @Test
        void 탈퇴하지_않은_유저면_성공한다() {
            // given
            Member member = registerAuthenticatedMember();

            RefreshToken refreshToken =
                    RefreshToken.builder()
                            .memberId(member.getId())
                            .token("testRefreshToken")
                            .build();
            refreshTokenRepository.save(refreshToken);

            // when
            memberService.withdrawalMember(member.getId().toString());
            Member currentMember = memberRepository.findById(member.getId()).get();

            // then
            assertThat(refreshTokenRepository.findById(member.getId())).isEmpty();
            assertThat(currentMember.getStatus()).isEqualTo(MemberStatus.DELETED);
        }

        @Test
        void 탈퇴한_유저면_예외가_발생한다() {
            // given
            Member member = registerAuthenticatedMember();
            memberService.withdrawalMember(member.getId().toString());

            // when & then
            assertThatThrownBy(() -> memberService.withdrawalMember(member.getId().toString()))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(MemberErrorCode.MEMBER_ALREADY_DELETED.getMessage());
        }
    }
}
