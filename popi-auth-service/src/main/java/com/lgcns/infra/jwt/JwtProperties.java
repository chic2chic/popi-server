package com.lgcns.infra.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String accessTokenSecret,
        String refreshTokenSecret,
        Long accessTokenExpirationTime,
        Long refreshTokenExpirationTime,
        String registerTokenSecret,
        Long registerTokenExpirationTime,
        String issuer) {
    public Long accessTokenExpirationMilliTime() {
        return accessTokenExpirationTime * 1000;
    }

    public Long refreshTokenExpirationMilliTime() {
        return refreshTokenExpirationTime * 1000;
    }

    public Long registerTokenExpirationMilliTime() {
        return registerTokenExpirationTime * 1000;
    }
}
