package com.lgcns.domain;

import static com.lgcns.constants.SecurityConstants.*;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OauthProvider {
    KAKAO(KAKAO_JWK_SET_URL, KAKAO_ISSUER),
    GOOGLE(GOOGLE_JWK_SET_URL, GOOGLE_ISSUER),
    ;

    private final String jwkSetUrl;
    private final String issuer;
}
