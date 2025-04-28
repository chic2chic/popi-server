package com.lgcns.controller;

import com.lgcns.domain.OauthProvider;
import com.lgcns.dto.request.AuthCodeRequest;
import com.lgcns.dto.response.SocialLoginResponse;
import com.lgcns.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증 서버 API", description = "인증 서버 API입니다.")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입 및 로그인", description = "회원가입 및 로그인을 진행합니다.")
    @PostMapping("/social-login")
    public SocialLoginResponse memberSocialLogin(
            @RequestParam(name = "oauthProvider") OauthProvider provider,
            @RequestBody AuthCodeRequest request) {
        return authService.socialLoginMember(request, provider);
    }
}
