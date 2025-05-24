package com.lgcns.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.WireMockIntegrationTest;
import com.lgcns.domain.OauthProvider;
import com.lgcns.dto.RegisterTokenDto;
import com.lgcns.dto.request.IdTokenRequest;
import com.lgcns.dto.request.MemberRegisterRequest;
import com.lgcns.dto.response.SocialLoginResponse;
import com.lgcns.enums.MemberAge;
import com.lgcns.enums.MemberGender;
import com.lgcns.enums.MemberRole;
import com.lgcns.error.exception.CustomException;
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

public class AuthServiceTest extends WireMockIntegrationTest {

    @Autowired AuthService authService;

    @MockitoBean JwtTokenService jwtTokenService;
    @MockitoBean IdTokenVerifier idTokenVerifier;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    class 회원가입할_때 {

        @Test
        void 아직_회원가입하지_않은_회원이라면_가입에_성공한다() throws JsonProcessingException {
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

            String expectedResponse =
                    objectMapper.writeValueAsString(Map.of("memberId", 1, "role", "USER"));

            stubFor(
                    post(urlEqualTo("/internal/register"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(expectedResponse)));

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
        void 이미_회원가입된_회원이_다시_회원가입하면_예외가_발생한다() throws JsonProcessingException {
            // given
            when(jwtTokenService.validateRegisterToken(anyString()))
                    .thenReturn(
                            RegisterTokenDto.of(
                                    "testOauthId", "testOauthProvider", "fake-register-token"));

            MemberRegisterRequest request =
                    new MemberRegisterRequest(
                            "testNickname", MemberAge.TWENTIES, MemberGender.MALE);

            String expectedResponse =
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "success",
                                    false,
                                    "status",
                                    409,
                                    "data",
                                    Map.of(
                                            "errorClassName", "ALREADY_REGISTERED",
                                            "message", "이미 가입된 사용자입니다. 로그인 후 이용해주세요."),
                                    "timestamp",
                                    "2025-05-22T00:07:44.787516"));

            stubFor(
                    post(urlEqualTo("/internal/register"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(409)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(expectedResponse)));

            // when & then
            assertThatThrownBy(() -> authService.registerMember("testRegisterTokenValue", request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("이미 가입된 사용자입니다. 로그인 후 이용해주세요.");
        }
    }

    @Nested
    class 소셜_로그인할_때 {

        @Test
        void 회원가입되지_않은_회원은_레지스터_토큰을_받는다() throws JsonProcessingException {
            // given
            when(idTokenVerifier.getOidcUser(anyString(), any())).thenReturn(mockOidcUser());
            when(jwtTokenService.createRegisterToken(anyString(), anyString()))
                    .thenReturn("fake-register-token");

            IdTokenRequest request = new IdTokenRequest("testIdTokenValue");

            String expectedResponse = objectMapper.writeValueAsString(null);

            stubFor(
                    post(urlEqualTo("/internal/oauth-info"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(expectedResponse)));

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
        void 이미_회원가입된_회원은_로그인에_성공한다() throws JsonProcessingException {
            when(idTokenVerifier.getOidcUser(anyString(), any())).thenReturn(mockOidcUser());
            when(jwtTokenService.createAccessToken(anyLong(), any(MemberRole.class)))
                    .thenReturn("fake-access-token");
            when(jwtTokenService.createRefreshToken(anyLong())).thenReturn("fake-refresh-token");

            IdTokenRequest request = new IdTokenRequest("testIdTokenValue");

            String expectedResponse =
                    objectMapper.writeValueAsString(
                            Map.of("memberId", 1, "role", "USER", "status", "NORMAL"));

            stubFor(
                    post(urlEqualTo("/internal/oauth-info"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(expectedResponse)));

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
