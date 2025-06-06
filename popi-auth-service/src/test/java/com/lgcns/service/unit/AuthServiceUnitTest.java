package com.lgcns.service.unit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.lgcns.client.MemberServiceClient;
import com.lgcns.domain.OauthProvider;
import com.lgcns.domain.RefreshToken;
import com.lgcns.dto.AccessTokenDto;
import com.lgcns.dto.RefreshTokenDto;
import com.lgcns.dto.RegisterTokenDto;
import com.lgcns.dto.request.IdTokenRequest;
import com.lgcns.dto.request.MemberInternalRegisterRequest;
import com.lgcns.dto.request.MemberRegisterRequest;
import com.lgcns.dto.response.MemberInternalInfoResponse;
import com.lgcns.dto.response.MemberInternalRegisterResponse;
import com.lgcns.dto.response.SocialLoginResponse;
import com.lgcns.dto.response.TokenReissueResponse;
import com.lgcns.enums.MemberAge;
import com.lgcns.enums.MemberGender;
import com.lgcns.enums.MemberRole;
import com.lgcns.enums.MemberStatus;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.AuthErrorCode;
import com.lgcns.repository.RefreshTokenRepository;
import com.lgcns.service.AuthServiceImpl;
import com.lgcns.service.IdTokenVerifier;
import com.lgcns.service.JwtTokenService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(MockitoExtension.class)
public class AuthServiceUnitTest {

    @InjectMocks AuthServiceImpl authService;
    @Mock RefreshTokenRepository refreshTokenRepository;

    @Mock MemberServiceClient memberServiceClient;

    @Mock JwtTokenService jwtTokenService;
    @Mock IdTokenVerifier idTokenVerifier;

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

    @Nested
    class 소셜_로그인할_때 {

        @Test
        void 회원가입되지_않은_회원은_레지스터_토큰을_받는다() {
            // given
            when(idTokenVerifier.getOidcUser(anyString(), any())).thenReturn(mockOidcUser());
            when(jwtTokenService.createRegisterToken(anyString(), anyString()))
                    .thenReturn("fake-register-token");

            IdTokenRequest request = new IdTokenRequest("testIdTokenValue");

            when(memberServiceClient.findByOauthInfo(any())).thenReturn(null);

            // when
            SocialLoginResponse response =
                    authService.socialLoginMember(OauthProvider.KAKAO, request);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.accessToken()).isNull(),
                    () -> assertThat(response.refreshToken()).isNull(),
                    () -> assertThat(response.registerToken()).isEqualTo("fake-register-token"),
                    () -> assertThat(response.isRegistered()).isFalse());
        }

        @Test
        void 이미_회원가입된_회원은_로그인에_성공한다() {
            when(idTokenVerifier.getOidcUser(anyString(), any())).thenReturn(mockOidcUser());
            when(jwtTokenService.createAccessToken(anyLong(), any(MemberRole.class)))
                    .thenReturn("fake-access-token");
            when(jwtTokenService.createRefreshToken(anyLong())).thenReturn("fake-refresh-token");

            IdTokenRequest request = new IdTokenRequest("testIdTokenValue");

            MemberInternalInfoResponse memberResponse =
                    new MemberInternalInfoResponse(
                            1L,
                            "최현태",
                            MemberAge.TWENTIES,
                            MemberGender.MALE,
                            MemberRole.USER,
                            MemberStatus.NORMAL);

            when(memberServiceClient.findByOauthInfo(any())).thenReturn(memberResponse);

            // when
            SocialLoginResponse response =
                    authService.socialLoginMember(OauthProvider.KAKAO, request);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.accessToken()).isEqualTo("fake-access-token"),
                    () -> assertThat(response.refreshToken()).isEqualTo("fake-refresh-token"),
                    () -> assertThat(response.registerToken()).isNull(),
                    () -> assertThat(response.isRegistered()).isTrue());
        }

        @Test
        void 회원_상태가_DELETED인_경우_재가입_처리_후_로그인에_성공한다() {
            when(idTokenVerifier.getOidcUser(anyString(), any())).thenReturn(mockOidcUser());
            when(jwtTokenService.createAccessToken(anyLong(), any(MemberRole.class)))
                    .thenReturn("fake-access-token");
            when(jwtTokenService.createRefreshToken(anyLong())).thenReturn("fake-refresh-token");

            IdTokenRequest request = new IdTokenRequest("testIdTokenValue");

            MemberInternalInfoResponse memberResponse =
                    new MemberInternalInfoResponse(
                            1L,
                            "최현태",
                            MemberAge.TWENTIES,
                            MemberGender.MALE,
                            MemberRole.USER,
                            MemberStatus.DELETED);

            when(memberServiceClient.findByOauthInfo(any())).thenReturn(memberResponse);
            doNothing().when(memberServiceClient).rejoinMember(1L);

            SocialLoginResponse response =
                    authService.socialLoginMember(OauthProvider.KAKAO, request);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.accessToken()).isEqualTo("fake-access-token"),
                    () -> assertThat(response.refreshToken()).isEqualTo("fake-refresh-token"),
                    () -> assertThat(response.registerToken()).isNull(),
                    () -> assertThat(response.isRegistered()).isTrue());
        }

        private OidcUser mockOidcUser() {
            OidcIdToken idToken =
                    new OidcIdToken(
                            "fake-id-token",
                            Instant.now(),
                            Instant.now().plusSeconds(3600),
                            Map.of(
                                    "sub", "test-subject",
                                    "iss", "https://test-issuer.example.com"));

            return new DefaultOidcUser(List.of(), idToken);
        }
    }

    @Nested
    class 토큰_재발급할_때 {

        @Test
        void 유효한_리프레시_토큰이면_새로운_토큰을_반환한다() {
            // given
            RefreshTokenDto oldRefreshTokenDto =
                    RefreshTokenDto.of(1L, "fake-old-register-token", 604800L);
            RefreshTokenDto newRefreshTokenDto =
                    RefreshTokenDto.of(1L, "fake-new-refresh-token", 604800L);
            AccessTokenDto newAccessTokenDto =
                    AccessTokenDto.of(1L, MemberRole.USER, "fake-new-access-token");

            when(jwtTokenService.validateRefreshToken(anyString())).thenReturn(oldRefreshTokenDto);
            when(jwtTokenService.reissueRefreshToken(oldRefreshTokenDto))
                    .thenReturn(newRefreshTokenDto);
            when(jwtTokenService.reissueAccessToken(1L, MemberRole.USER))
                    .thenReturn(newAccessTokenDto);

            MemberInternalInfoResponse memberResponse =
                    new MemberInternalInfoResponse(
                            1L,
                            "최현태",
                            MemberAge.TWENTIES,
                            MemberGender.MALE,
                            MemberRole.USER,
                            MemberStatus.DELETED);

            when(memberServiceClient.findByMemberId(1L)).thenReturn(memberResponse);

            // when
            TokenReissueResponse response = authService.reissueToken("testRefreshTokenValue");

            // then
            Assertions.assertAll(
                    () -> assertThat(response.accessToken()).isEqualTo("fake-new-access-token"),
                    () -> assertThat(response.refreshToken()).isEqualTo("fake-new-refresh-token"));
        }

        @Test
        void 만료된_리프레시_토큰이면_예외가_발생한다() {
            // given
            when(jwtTokenService.validateRefreshToken(anyString())).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> authService.reissueToken("testRefreshTokenValue"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AuthErrorCode.EXPIRED_REFRESH_TOKEN.getMessage());
        }
    }

    @Nested
    class 로그아웃할_때 {

        @Test
        void 리프레시_토큰이_존재하면_삭제된다() {
            // given
            RefreshToken refreshToken =
                    RefreshToken.builder().memberId(1L).token("testRefreshToken").build();
            refreshTokenRepository.save(refreshToken);

            // when
            authService.logoutMember(String.valueOf(1L));

            // then
            assertThat(refreshTokenRepository.findById(1L)).isEmpty();
        }
    }
}
