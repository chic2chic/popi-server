package com.lgcns.util;

import org.springframework.boot.web.server.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    public HttpHeaders deleteRefreshTokenCookie() {
        ResponseCookie refreshTokenCookie =
                ResponseCookie.from("refreshToken", "")
                        .path("/")
                        .maxAge(0)
                        .secure(true)
                        .sameSite(Cookie.SameSite.NONE.attributeValue())
                        .httpOnly(true)
                        .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        return headers;
    }
}
