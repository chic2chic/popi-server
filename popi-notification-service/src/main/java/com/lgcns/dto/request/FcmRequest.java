package com.lgcns.dto.request;

public record FcmRequest(String title, String body, String fcmToken, String key) {
    public static FcmRequest of(String title, String body, String fcmToken) {
        return new FcmRequest(title, body, fcmToken, "redirectUrl"); // 추후 redirectUrl 생성 로직 구현
    }
}
