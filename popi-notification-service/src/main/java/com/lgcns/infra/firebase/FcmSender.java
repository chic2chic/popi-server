package com.lgcns.infra.firebase;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.lgcns.dto.request.FcmRequest;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.FirebaseErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FcmSender {

    private final FirebaseMessaging firebaseMessaging;

    public void sendFcm(FcmRequest fcmRequest) {
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

        try {
            firebaseMessaging.send(message);
        } catch (FirebaseMessagingException e) {
            log.info("FCM ERROR : {}", e.getMessage());
            throw new CustomException(FirebaseErrorCode.FCM_SEND_FAILED);
        }
    }
}
