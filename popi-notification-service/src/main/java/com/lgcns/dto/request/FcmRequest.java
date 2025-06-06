package com.lgcns.dto.request;

public record FcmRequest(String title, String body, String fcmToken, String key) {
    public static FcmRequest of(String fcmToken) {
        return new FcmRequest(
                "PoPI 예약 알림",
                "예약하신 팝업이 1시간 뒤에 시작돼요. 잊지 말고 방문해 주세요!",
                fcmToken,
                "redirectUrl"); // 추후 redirectUrl 생성 로직 구현
    }
}
