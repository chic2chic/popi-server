package com.lgcns.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SocialLoginResponse(
        @Schema(description = "엑세스 토큰") String accessToken,
        @Schema(description = "리프레시 토큰") String refreshToken) {
    public static SocialLoginResponse of(String accessToken, String refreshToken) {
        return new SocialLoginResponse(accessToken, refreshToken);
    }
}
