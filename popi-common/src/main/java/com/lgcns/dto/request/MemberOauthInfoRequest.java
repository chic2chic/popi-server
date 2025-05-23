package com.lgcns.dto.request;

public record MemberOauthInfoRequest(String oauthId, String oauthProvider) {
    public static MemberOauthInfoRequest of(String oauthId, String oauthProvider) {
        return new MemberOauthInfoRequest(oauthId, oauthProvider);
    }
}
