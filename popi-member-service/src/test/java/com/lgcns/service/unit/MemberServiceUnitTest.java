package com.lgcns.service.unit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

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

    @Nested
    class 회원_정보를_조회할_때 {

        @Test
        void 회원이_존재하면_조회에_성공한다() {
            // given
            Member member =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname",
                            MemberGender.MALE,
                            MemberAge.TWENTIES);

            ReflectionTestUtils.setField(member, "id", 1L);

            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

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
}
