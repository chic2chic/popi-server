package com.lgcns.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

public record SocialLoginResponse(
        @Schema(description = "엑세스 토큰") String accessToken,
        @JsonIgnore @Schema(description = "리프레시 토큰") String refreshToken,
        @Schema(description = "레지스터 토큰") String registerToken,
        @Schema(description = "신규 유저 등록 여부") boolean isRegistered) {
    public static SocialLoginResponse registered(String accessToken, String refreshToken) {
        return new SocialLoginResponse(accessToken, refreshToken, null, true);
    }

    public static SocialLoginResponse notRegistered(String registerToken) {
        return new SocialLoginResponse(null, null, registerToken, false);
    }
}
