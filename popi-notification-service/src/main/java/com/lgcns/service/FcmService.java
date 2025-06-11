package com.lgcns.service;

import com.google.api.core.ApiFuture;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

@Service
public class FcmService {

    private static final String NOTIFICATION_TITLE = "PoPI 예약 알림";
    private static final String NOTIFICATION_BODY = "예약하신 팝업이 1시간 뒤에 시작돼요. 잊지 말고 방문해 주세요!";

    public ApiFuture<String> sendMessageSync(String fcmToken) {

        Notification notification =
                Notification.builder()
                        .setTitle(NOTIFICATION_TITLE)
                        .setBody(NOTIFICATION_BODY)
                        .build();

        Message message =
                Message.builder().setToken(fcmToken).setNotification(notification).build();

        return FirebaseMessaging.getInstance().sendAsync(message);
    }
}
