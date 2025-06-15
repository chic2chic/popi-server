package com.lgcns.service.unit;

import static com.lgcns.grpc.mapper.MemberGrpcMapper.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.lgcns.client.MemberGrpcClient;
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
import com.lgcns.enums.MemberStatus;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.AuthErrorCode;
import com.lgcns.repository.RefreshTokenRepository;
import com.lgcns.service.AuthServiceImpl;
import com.lgcns.service.IdTokenVerifier;
import com.lgcns.service.JwtTokenService;
import com.popi.common.grpc.auth.RefreshTokenDeleteRequest;
import com.popi.common.grpc.member.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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

    @InjectMocks private AuthServiceImpl authService;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    @Mock private MemberGrpcClient memberGrpcClient;

    @Mock private JwtTokenService jwtTokenService;
    @Mock private IdTokenVerifier idTokenVerifier;

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

            MemberInternalRegisterResponse grpcResponse =
                    MemberInternalRegisterResponse.newBuilder()
                            .setMemberId(1L)
                            .setRole(toGrpcMemberRole(MemberRole.USER))
                            .build();

            given(memberGrpcClient.registerMember(any(MemberInternalRegisterRequest.class)))
                    .willReturn(grpcResponse);

            // when
            SocialLoginResponse response =
                    authService.registerMember("testRegisterTokenValue", request);

            // then
            verify(memberGrpcClient, times(1))
                    .registerMember(any(MemberInternalRegisterRequest.class));
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
                                    "testOauthId", "testOauthProvider", "fake-register-token"));

            MemberRegisterRequest request =
                    new MemberRegisterRequest(
                            "testNickname", MemberAge.TWENTIES, MemberGender.MALE);

            given(memberGrpcClient.registerMember(any(MemberInternalRegisterRequest.class)))
                    .willThrow(new RuntimeException("이미 가입된 사용자입니다. 로그인 후 이용해주세요."));

            // when & then
            assertThatThrownBy(() -> authService.registerMember("testRegisterTokenValue", request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("이미 가입된 사용자입니다. 로그인 후 이용해주세요.");
            verify(memberGrpcClient, times(1))
                    .registerMember(any(MemberInternalRegisterRequest.class));
        }

        @Test
        void 만료된_레지스터_토큰이면_예외가_발생한다() {
            // given
            given(jwtTokenService.validateRegisterToken(anyString())).willReturn(null);

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
            given(idTokenVerifier.getOidcUser(anyString(), any())).willReturn(mockOidcUser());
            given(jwtTokenService.createRegisterToken(anyString(), anyString()))
                    .willReturn("fake-register-token");

            IdTokenRequest request = new IdTokenRequest("testIdTokenValue");

            given(memberGrpcClient.findByOauthInfo(any()))
                    .willThrow(new StatusRuntimeException(Status.NOT_FOUND));

            SocialLoginResponse response =
                    authService.socialLoginMember(OauthProvider.KAKAO, request);

            // then
            verify(memberGrpcClient, times(1)).findByOauthInfo(any());
            Assertions.assertAll(
                    () -> assertThat(response.accessToken()).isNull(),
                    () -> assertThat(response.refreshToken()).isNull(),
                    () -> assertThat(response.registerToken()).isEqualTo("fake-register-token"),
                    () -> assertThat(response.isRegistered()).isFalse());
        }

        @Test
        void 이미_회원가입된_회원은_로그인에_성공한다() {
            given(idTokenVerifier.getOidcUser(anyString(), any())).willReturn(mockOidcUser());
            given(jwtTokenService.createAccessToken(anyLong(), any(MemberRole.class)))
                    .willReturn("fake-access-token");
            given(jwtTokenService.createRefreshToken(anyLong())).willReturn("fake-refresh-token");

            IdTokenRequest request = new IdTokenRequest("testIdTokenValue");

            MemberInternalInfoResponse grpcResponse =
                    MemberInternalInfoResponse.newBuilder()
                            .setMemberId(1L)
                            .setNickname("최현태")
                            .setAge(toGrpcMemberAge(MemberAge.TWENTIES))
                            .setGender(toGrpcMemberGender(MemberGender.MALE))
                            .setRole(toGrpcMemberRole(MemberRole.USER))
                            .setStatus(toGrpcMemberStatus(MemberStatus.NORMAL))
                            .build();

            given(memberGrpcClient.findByOauthInfo(any())).willReturn(grpcResponse);

            // when
            SocialLoginResponse response =
                    authService.socialLoginMember(OauthProvider.KAKAO, request);

            // then
            verify(memberGrpcClient, times(1))
                    .findByOauthInfo(any(MemberInternalOauthInfoRequest.class));
            Assertions.assertAll(
                    () -> assertThat(response.accessToken()).isEqualTo("fake-access-token"),
                    () -> assertThat(response.refreshToken()).isEqualTo("fake-refresh-token"),
                    () -> assertThat(response.registerToken()).isNull(),
                    () -> assertThat(response.isRegistered()).isTrue());
        }

        @Test
        void 회원_상태가_DELETED인_경우_재가입_처리_후_로그인에_성공한다() {
            given(idTokenVerifier.getOidcUser(anyString(), any())).willReturn(mockOidcUser());
            given(jwtTokenService.createAccessToken(anyLong(), any(MemberRole.class)))
                    .willReturn("fake-access-token");
            given(jwtTokenService.createRefreshToken(anyLong())).willReturn("fake-refresh-token");

            IdTokenRequest request = new IdTokenRequest("testIdTokenValue");

            MemberInternalInfoResponse grpcResponse =
                    MemberInternalInfoResponse.newBuilder()
                            .setMemberId(1L)
                            .setNickname("최현태")
                            .setAge(toGrpcMemberAge(MemberAge.TWENTIES))
                            .setGender(toGrpcMemberGender(MemberGender.MALE))
                            .setRole(toGrpcMemberRole(MemberRole.USER))
                            .setStatus(toGrpcMemberStatus(MemberStatus.DELETED))
                            .build();

            given(memberGrpcClient.findByOauthInfo(any())).willReturn(grpcResponse);

            SocialLoginResponse response =
                    authService.socialLoginMember(OauthProvider.KAKAO, request);

            // then
            verify(memberGrpcClient, times(1))
                    .findByOauthInfo(any(MemberInternalOauthInfoRequest.class));
            verify(memberGrpcClient, times(1))
                    .rejoinMember(MemberInternalIdRequest.newBuilder().setMemberId(1L).build());
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

            given(jwtTokenService.validateRefreshToken(anyString())).willReturn(oldRefreshTokenDto);
            given(jwtTokenService.reissueRefreshToken(oldRefreshTokenDto))
                    .willReturn(newRefreshTokenDto);
            given(jwtTokenService.reissueAccessToken(1L, MemberRole.USER))
                    .willReturn(newAccessTokenDto);

            MemberInternalInfoResponse grpcResponse =
                    MemberInternalInfoResponse.newBuilder()
                            .setMemberId(1L)
                            .setNickname("최현태")
                            .setAge(toGrpcMemberAge(MemberAge.TWENTIES))
                            .setGender(toGrpcMemberGender(MemberGender.MALE))
                            .setRole(toGrpcMemberRole(MemberRole.USER))
                            .setStatus(toGrpcMemberStatus(MemberStatus.DELETED))
                            .build();

            when(memberGrpcClient.findByMemberId(any(MemberInternalIdRequest.class)))
                    .thenReturn(grpcResponse);

            // when
            TokenReissueResponse response = authService.reissueToken("testRefreshTokenValue");

            // then
            verify(memberGrpcClient, times(1)).findByMemberId(any(MemberInternalIdRequest.class));
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
            RefreshToken refreshToken =
                    RefreshToken.builder().memberId(1L).token("testRefreshToken").build();
            refreshTokenRepository.save(refreshToken);

            // when
            authService.deleteRefreshToken(
                    RefreshTokenDeleteRequest.newBuilder().setMemberId("1").build());

            // then
            assertThat(refreshTokenRepository.findById(1L)).isEmpty();
        }
    }
}
