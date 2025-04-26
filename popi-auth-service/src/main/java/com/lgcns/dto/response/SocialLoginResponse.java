package com.lgcns.dto.response;

public record SocialLoginResponse(String accessToken, String refreshToken) {
    public static SocialLoginResponse of(String accessToken, String refreshToken) {
        return new SocialLoginResponse(accessToken, refreshToken);
    }
}
