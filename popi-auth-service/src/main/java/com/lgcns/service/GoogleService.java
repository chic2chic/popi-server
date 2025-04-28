package com.lgcns.service;

import com.lgcns.client.GoogleOauthClient;
import com.lgcns.dto.response.IdTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleService {

    private final GoogleOauthClient googleOauthClient;

    @Value("${oauth.google.client-id}")
    private String googleClientId;

    @Value("${oauth.google.client-secret}")
    private String googleClientSecret;

    @Value("${oauth.google.redirect-uri}")
    private String googleRedirectUri;

    @Value("${oauth.google.grant-type}")
    private String googleGrantType;

    public String getIdToken(String code) {
        IdTokenResponse response =
                googleOauthClient.getIdToken(
                        googleGrantType,
                        googleClientId,
                        googleRedirectUri,
                        code,
                        googleClientSecret);

        return response.id_token();
    }
}
