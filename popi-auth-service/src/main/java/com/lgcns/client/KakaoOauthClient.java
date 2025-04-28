package com.lgcns.client;

import static com.lgcns.constants.SecurityConstants.KAKAO_LOGIN_ENDPOINT;
import static com.lgcns.constants.SecurityConstants.KAKAO_LOGIN_URL;

import com.lgcns.config.FeignConfig;
import com.lgcns.dto.response.IdTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "kakaoOauthClient", url = KAKAO_LOGIN_URL, configuration = FeignConfig.class)
public interface KakaoOauthClient {
    @PostMapping(value = KAKAO_LOGIN_ENDPOINT)
    IdTokenResponse getIdToken(
            @RequestParam("grant_type") String grantType,
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("code") String code,
            @RequestParam("client_secret") String clientSecret);
}
