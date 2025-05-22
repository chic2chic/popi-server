package com.lgcns.dto;

public record RegisterTokenDto(String oauthId, String oauthProvider, String registerTokenValue) {
    public static RegisterTokenDto of(
            String oauthId, String oauthProvider, String registerTokenValue) {
        return new RegisterTokenDto(oauthId, oauthProvider, registerTokenValue);
    }
}
