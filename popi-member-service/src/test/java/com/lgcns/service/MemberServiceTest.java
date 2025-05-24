package com.lgcns.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.lgcns.WireMockIntegrationTest;
import com.lgcns.domain.Member;
import com.lgcns.domain.OauthInfo;
import com.lgcns.dto.response.MemberInfoResponse;
import com.lgcns.enums.MemberAge;
import com.lgcns.enums.MemberGender;
import com.lgcns.enums.MemberRole;
import com.lgcns.enums.MemberStatus;
import com.lgcns.repository.MemberRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MemberServiceTest extends WireMockIntegrationTest {

    @Autowired private MemberService memberService;
    @Autowired private MemberRepository memberRepository;

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

    @Test
    void 회원_정보를_조회한다() {
        // given
        registerAuthenticatedMember();

        // when
        MemberInfoResponse response = memberService.findMemberInfo("1");

        // then
        Assertions.assertAll(
                () -> assertThat(response.memberId()).isEqualTo(1L),
                () -> assertThat(response.nickname()).isEqualTo("testNickname"),
                () -> assertThat(response.role()).isEqualTo(MemberRole.USER),
                () -> assertThat(response.gender()).isEqualTo(MemberGender.MALE),
                () -> assertThat(response.age()).isEqualTo(MemberAge.TWENTIES),
                () -> assertThat(response.status()).isEqualTo(MemberStatus.NORMAL));
    }
}
