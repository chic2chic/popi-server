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
import com.lgcns.dto.RegisterTokenDto;
import com.lgcns.dto.request.MemberRegisterRequest;
import com.lgcns.dto.response.SocialLoginResponse;
import com.lgcns.enums.MemberAge;
import com.lgcns.enums.MemberGender;
import com.lgcns.enums.MemberRole;
import com.lgcns.error.exception.CustomException;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class AuthServiceTest extends WireMockIntegrationTest {

    @Autowired AuthService authService;

    @MockitoBean JwtTokenService jwtTokenService;

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
}
