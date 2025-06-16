package com.lgcns.service.integration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.lgcns.domain.OauthProvider;
import com.lgcns.domain.RefreshToken;
import com.lgcns.dto.AccessTokenDto;
import com.lgcns.dto.RefreshTokenDto;
import com.lgcns.dto.RegisterTokenDto;
import com.lgcns.dto.request.IdTokenRequest;
import com.lgcns.dto.request.MemberRegisterRequest;
import com.lgcns.dto.response.SocialLoginResponse;
import com.lgcns.dto.response.TokenReissueResponse;
import com.lgcns.enums.MemberAge;
import com.lgcns.enums.MemberGender;
import com.lgcns.enums.MemberRole;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.AuthErrorCode;
import com.lgcns.repository.RefreshTokenRepository;
import com.lgcns.service.AuthService;
import com.lgcns.service.IdTokenVerifier;
import com.lgcns.service.JwtTokenService;
import com.popi.common.grpc.auth.RefreshTokenDeleteRequest;
import io.grpc.StatusRuntimeException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class AuthServiceIntegrationTest extends GrpcIntegrationTest {

    @Autowired private AuthService authService;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean private JwtTokenService jwtTokenService;
    @MockitoBean private IdTokenVerifier idTokenVerifier;

    @Nested
    class 회원가입할_때 {

        @Test
        void 아직_회원가입하지_않은_회원이라면_가입에_성공한다() {
            // given
            given(jwtTokenService.validateRegisterToken(anyString()))
                    .willReturn(
                            RegisterTokenDto.of(
                                    "testOauthId", "testOauthProvider", "fake-register-token"));
            given(jwtTokenService.createAccessToken(anyLong(), any(MemberRole.class)))
                    .willReturn("fake-access-token");
            given(jwtTokenService.createRefreshToken(anyLong())).willReturn("fake-refresh-token");

            MemberRegisterRequest request =
                    new MemberRegisterRequest(
                            "testNickname", MemberAge.TWENTIES, MemberGender.MALE);

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
            given(jwtTokenService.validateRegisterToken(anyString()))
                    .willReturn(
                            RegisterTokenDto.of(
                                    "alreadyRegisteredOauthId",
                                    "testOauthProvider",
                                    "fake-register-token"));

            MemberRegisterRequest request =
                    new MemberRegisterRequest(
                            "testNickname", MemberAge.TWENTIES, MemberGender.MALE);

            // when & then
            assertThatThrownBy(() -> authService.registerMember("testRegisterTokenValue", request))
                    .isInstanceOf(StatusRuntimeException.class)
                    .hasMessageContaining("ALREADY_EXISTS");
        }

        @Test
        void 만료된_레지스터_토큰이면_예외가_발생한다() {
            // given
            MemberRegisterRequest request =
                    new MemberRegisterRequest(
                            "testNickname", MemberAge.TWENTIES, MemberGender.MALE);

            // when & then
            assertThatThrownBy(() -> authService.registerMember("testRefreshTokenValue", request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AuthErrorCode.EXPIRED_REGISTER_TOKEN.getMessage());
            verify(jwtTokenService, times(1)).validateRegisterToken(anyString());
        }
    }

    @Nested
    class 소셜_로그인할_때 {

        @Test
        void 회원가입되지_않은_회원은_레지스터_토큰을_받는다() {
            // given
            given(idTokenVerifier.getOidcUser(anyString(), any()))
                    .willReturn(mockOidcUser("not-registered-oauthId"));
            given(jwtTokenService.createRegisterToken(anyString(), anyString()))
                    .willReturn("fake-register-token");

            IdTokenRequest request = new IdTokenRequest("testIdTokenValue");

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
            given(idTokenVerifier.getOidcUser(anyString(), any()))
                    .willReturn(mockOidcUser("already-registered-oauthId"));
            given(jwtTokenService.createAccessToken(anyLong(), any(MemberRole.class)))
                    .willReturn("fake-access-token");
            given(jwtTokenService.createRefreshToken(anyLong())).willReturn("fake-refresh-token");

            IdTokenRequest request = new IdTokenRequest("testIdTokenValue");

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
            given(idTokenVerifier.getOidcUser(anyString(), any()))
                    .willReturn(mockOidcUser("deleted-oauthId"));
            given(jwtTokenService.createAccessToken(anyLong(), any(MemberRole.class)))
                    .willReturn("fake-access-token");
            given(jwtTokenService.createRefreshToken(anyLong())).willReturn("fake-refresh-token");

            IdTokenRequest request = new IdTokenRequest("testIdTokenValue");

            SocialLoginResponse response =
                    authService.socialLoginMember(OauthProvider.KAKAO, request);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.accessToken()).isEqualTo("fake-access-token"),
                    () -> assertThat(response.refreshToken()).isEqualTo("fake-refresh-token"),
                    () -> assertThat(response.registerToken()).isNull(),
                    () -> assertThat(response.isRegistered()).isTrue());
        }
    }

    private OidcUser mockOidcUser(String sub) {
        OidcIdToken idToken =
                new OidcIdToken(
                        "fake-id-token",
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        Map.of("sub", sub, "iss", "https://test-issuer.example.com"));

        return new DefaultOidcUser(List.of(), idToken);
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

            given(jwtTokenService.validateRefreshToken(anyString())).willReturn(oldRefreshTokenDto);
            given(jwtTokenService.reissueRefreshToken(oldRefreshTokenDto))
                    .willReturn(newRefreshTokenDto);
            given(jwtTokenService.reissueAccessToken(1L, MemberRole.USER))
                    .willReturn(newAccessTokenDto);

            // when
            TokenReissueResponse response = authService.reissueToken("testRefreshTokenValue");

            // then
            Assertions.assertAll(
                    () -> assertThat(response.accessToken()).isEqualTo("fake-new-access-token"),
                    () -> assertThat(response.refreshToken()).isEqualTo("fake-new-refresh-token"));
        }

        @Test
        void 만료된_리프레시_토큰이면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> authService.reissueToken("testRefreshTokenValue"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AuthErrorCode.EXPIRED_REFRESH_TOKEN.getMessage());
            verify(jwtTokenService, times(1)).validateRefreshToken(anyString());
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

    @Nested
    class 회원_서비스의_토큰_삭제_요청을_처리할_때 {
        @Test
        void 리프레시_토큰이_존재하면_삭제된다() {
            // given
            RefreshTokenDeleteRequest grpcRequest =
                    RefreshTokenDeleteRequest.newBuilder().setMemberId("1").build();

            RefreshToken refreshToken =
                    RefreshToken.builder().memberId(1L).token("testRefreshToken").build();
            refreshTokenRepository.save(refreshToken);

            // when
            authService.deleteRefreshToken(grpcRequest);

            // then
            assertThat(refreshTokenRepository.findById(1L)).isEmpty();
        }
    }
}
