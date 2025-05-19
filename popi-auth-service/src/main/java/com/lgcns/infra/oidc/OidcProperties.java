package com.lgcns.infra.oidc;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oidc")
public record OidcProperties(KAKAO kakao, GOOGLE google) {
    public record KAKAO(String jwkSetUri, String issuer, String audience) {}

    public record GOOGLE(String jwkSetUri, String issuer, String audience) {}
}
