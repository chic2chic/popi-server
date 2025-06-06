package com.lgcns.service.unit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import com.lgcns.client.MemberServiceClient;
import com.lgcns.dto.RegisterTokenDto;
import com.lgcns.dto.request.MemberInternalRegisterRequest;
import com.lgcns.dto.request.MemberRegisterRequest;
import com.lgcns.dto.response.MemberInternalRegisterResponse;
import com.lgcns.dto.response.SocialLoginResponse;
import com.lgcns.enums.MemberAge;
import com.lgcns.enums.MemberGender;
import com.lgcns.enums.MemberRole;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.AuthErrorCode;
import com.lgcns.service.AuthServiceImpl;
import com.lgcns.service.JwtTokenService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuthServiceUnitTest {

    @InjectMocks AuthServiceImpl authService;

    @Mock MemberServiceClient memberServiceClient;

    @Mock JwtTokenService jwtTokenService;

    @Nested
    class 회원가입할_때 {

        @Test
        void 아직_회원가입하지_않은_회원이라면_가입에_성공한다() {
            // given
            when(jwtTokenService.validateRegisterToken(anyString()))
                    .thenReturn(
                            RegisterTokenDto.of(
                                    "testOauthId", "testOauthProvider", "fake-register-token"));
            when(jwtTokenService.createAccessToken(anyLong(), any(MemberRole.class)))
                    .thenReturn("fake-access-token");
            when(jwtTokenService.createRefreshToken(anyLong())).thenReturn("fake-refresh-token");

            MemberRegisterRequest request =
                    new MemberRegisterRequest(
                            "testNickname", MemberAge.TWENTIES, MemberGender.MALE);

            MemberInternalRegisterResponse memberResponse =
                    new MemberInternalRegisterResponse(1L, MemberRole.USER);

            when(memberServiceClient.registerMember(any(MemberInternalRegisterRequest.class)))
                    .thenReturn(memberResponse);

            // when
            SocialLoginResponse response =
                    authService.registerMember("testRegisterTokenValue", request);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.accessToken()).isEqualTo("fake-access-token"),
                    () -> assertThat(response.refreshToken()).isEqualTo("fake-refresh-token"),
                    () -> assertThat(response.registerToken()).isNull(),
                    () -> assertThat(response.isRegistered()).isTrue());
        }

        @Test
        void 이미_회원가입된_회원이_다시_회원가입하면_예외가_발생한다() {
            // given
            when(jwtTokenService.validateRegisterToken(anyString()))
                    .thenReturn(
                            RegisterTokenDto.of(
                                    "testOauthId", "testOauthProvider", "fake-register-token"));

            MemberRegisterRequest request =
                    new MemberRegisterRequest(
                            "testNickname", MemberAge.TWENTIES, MemberGender.MALE);

            when(memberServiceClient.registerMember(any()))
                    .thenThrow(new RuntimeException("이미 가입된 사용자입니다. 로그인 후 이용해주세요."));

            // when & then
            assertThatThrownBy(() -> authService.registerMember("testRegisterTokenValue", request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("이미 가입된 사용자입니다. 로그인 후 이용해주세요.");
        }

        @Test
        void 만료된_레지스터_토큰이면_예외가_발생한다() {
            // given
            when(jwtTokenService.validateRegisterToken(anyString())).thenReturn(null);

            MemberRegisterRequest request =
                    new MemberRegisterRequest(
                            "testNickname", MemberAge.TWENTIES, MemberGender.MALE);

            // when & then
            assertThatThrownBy(() -> authService.registerMember("testRefreshTokenValue", request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AuthErrorCode.EXPIRED_REGISTER_TOKEN.getMessage());
        }
    }
}
