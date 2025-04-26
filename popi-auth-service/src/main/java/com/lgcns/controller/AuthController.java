package com.lgcns.controller;

import com.lgcns.domain.OauthProvider;
import com.lgcns.dto.request.AuthCodeRequest;
import com.lgcns.dto.response.SocialLoginResponse;
import com.lgcns.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/social-login")
    public SocialLoginResponse memberSocialLogin(
            @RequestParam(name = "oauthProvider") OauthProvider provider,
            @RequestBody AuthCodeRequest request) {
        return authService.socialLoginMember(request, provider);
    }
}
