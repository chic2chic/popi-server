package com.lgcns.service;

import com.lgcns.client.KakaoOauthClient;
import com.lgcns.dto.response.IdTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KakaoService {

    private final KakaoOauthClient kakaoOauthClient;

    @Value("${oauth.kakao.client-id}")
    private String kakaoClientId;

    @Value("${oauth.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${oauth.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${oauth.kakao.grant-type}")
    private String kakaoGrantType;

    public String getIdToken(String code) {
        IdTokenResponse response =
                kakaoOauthClient.getIdToken(
                        kakaoGrantType, kakaoClientId, kakaoRedirectUri, code, kakaoClientSecret);

        return response.id_token();
    }
}
