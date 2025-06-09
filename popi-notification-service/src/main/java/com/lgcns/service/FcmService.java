package com.lgcns.service;

import com.google.api.core.ApiFuture;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.lgcns.dto.request.FcmRequest;
import org.springframework.stereotype.Service;

@Service
public class FcmService {

    public ApiFuture<String> sendMessageSync(FcmRequest fcmRequest) {
        if (fcmRequest.fcmToken() == null || fcmRequest.fcmToken().isEmpty()) {
            return null;
        }

        Notification notification =
                Notification.builder()
                        .setTitle(fcmRequest.title())
                        .setBody(fcmRequest.body())
                        .build();

        Message message =
                Message.builder()
                        .setToken(fcmRequest.fcmToken())
                        .setNotification(notification)
                        // .putData("key", fcmRequest.key()) // 추후 redirectUrl 추가하면서 리팩토링
                        .build();

        return FirebaseMessaging.getInstance().sendAsync(message);
    }
}
