package com.lgcns.controller;

import com.lgcns.domain.OauthProvider;
import com.lgcns.dto.response.SocialLoginResponse;
import com.lgcns.dto.response.TokenReissueResponse;
import com.lgcns.service.AuthService;
import com.lgcns.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증 서버 API", description = "인증 서버 API입니다.")
@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @Operation(summary = "회원가입 및 로그인", description = "회원가입 및 로그인을 진행합니다.")
    @PostMapping("/social-login")
    public ResponseEntity<SocialLoginResponse> memberSocialLogin(
            @RequestParam(name = "oauthProvider") OauthProvider provider) {
        SocialLoginResponse response = authService.socialLoginMember(provider);

        String refreshToken = response.refreshToken();
        HttpHeaders headers = cookieUtil.generateRefreshTokenCookie(refreshToken);

        return ResponseEntity.ok().headers(headers).body(response);
    }

    @Operation(
            summary = "토큰 재발급",
            description = "만료된 엑세스 토큰이 있을 경우, 리프레시 토큰을 이용해 엑세스 및 리프레시 토큰을 재발급합니다.")
    @PostMapping("/reissue")
    public ResponseEntity<TokenReissueResponse> tokenReissue(
            @RequestHeader("refresh-token") String refreshTokenValue) {
        TokenReissueResponse response = authService.reissueToken(refreshTokenValue);

        String refreshToken = response.refreshToken();
        HttpHeaders headers = cookieUtil.generateRefreshTokenCookie(refreshToken);

        return ResponseEntity.ok().headers(headers).body(response);
    }
}
