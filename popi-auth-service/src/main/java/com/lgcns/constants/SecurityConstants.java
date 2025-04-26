package com.lgcns.constants;

public final class SecurityConstants {
    public static final String TOKEN_ROLE_NAME = "role";

    public static final String KAKAO_LOGIN_URL = "https://kauth.kakao.com";
    public static final String KAKAO_LOGIN_ENDPOINT = "/oauth/token";

    public static final String GOOGLE_LOGIN_URL = "https://oauth2.googleapis.com";
    public static final String GOOGLE_LOGIN_ENDPOINT = "/token";

    public static final String KAKAO_JWK_SET_URL = "https://kauth.kakao.com/.well-known/jwks.json";
    public static final String KAKAO_ISSUER = "https://kauth.kakao.com";

    public static final String GOOGLE_JWK_SET_URL = "https://www.googleapis.com/oauth2/v3/certs";
    public static final String GOOGLE_ISSUER = "https://accounts.google.com";
}
