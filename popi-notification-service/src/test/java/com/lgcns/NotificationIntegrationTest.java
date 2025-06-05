package com.lgcns;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.lgcns.infra.firebase.FcmSender;
import com.lgcns.repository.FcmDeviceRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
public class NotificationIntegrationTest {
    @MockitoBean protected FcmSender fcmSender;
    @MockitoBean protected FirebaseMessaging firebaseMessaging;
    @MockitoBean protected FirebaseApp firebaseApp;
    @MockitoBean protected FcmDeviceRepository fcmDeviceRepository;
}
