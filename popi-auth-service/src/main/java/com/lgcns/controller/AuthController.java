package com.lgcns.controller;

import com.lgcns.domain.OauthProvider;
import com.lgcns.dto.request.IdTokenRequest;
import com.lgcns.dto.response.SocialLoginResponse;
import com.lgcns.dto.response.TokenReissueResponse;
import com.lgcns.service.AuthService;
import com.lgcns.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "인증 서버 API", description = "인증 서버 API입니다.")
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @PostMapping("/social-login")
    @Operation(summary = "회원가입 및 로그인", description = "회원가입 및 로그인을 진행합니다.")
    public ResponseEntity<SocialLoginResponse> memberSocialLogin(
            @RequestParam(name = "oauthProvider") OauthProvider provider,
            @Valid IdTokenRequest request) {
        SocialLoginResponse response = authService.socialLoginMember(provider, request);

        String refreshToken = response.refreshToken();
        HttpHeaders headers = cookieUtil.generateRefreshTokenCookie(refreshToken);

        return ResponseEntity.ok().headers(headers).body(response);
    }

    @PostMapping("/reissue")
    @Operation(
            summary = "토큰 재발급",
            description = "만료된 엑세스 토큰이 있을 경우, 리프레시 토큰을 이용해 엑세스 및 리프레시 토큰을 재발급합니다.")
    public ResponseEntity<TokenReissueResponse> tokenReissue(
            @RequestHeader("refresh-token") String refreshTokenValue) {
        TokenReissueResponse response = authService.reissueToken(refreshTokenValue);

        String refreshToken = response.refreshToken();
        HttpHeaders headers = cookieUtil.generateRefreshTokenCookie(refreshToken);

        return ResponseEntity.ok().headers(headers).body(response);
    }
}
