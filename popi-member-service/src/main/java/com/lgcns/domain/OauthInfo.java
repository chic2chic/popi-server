package com.lgcns.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OauthInfo {

    private String oauthId;
    private String oauthProvider;

    @Builder(access = AccessLevel.PRIVATE)
    private OauthInfo(String oauthId, String oauthProvider) {
        this.oauthId = oauthId;
        this.oauthProvider = oauthProvider;
    }

    public static OauthInfo createOauthInfo(String oauthId, String oauthProvider) {
        return OauthInfo.builder().oauthId(oauthId).oauthProvider(oauthProvider).build();
    }
}
