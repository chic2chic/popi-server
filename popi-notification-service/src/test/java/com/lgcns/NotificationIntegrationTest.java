package com.lgcns;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.lgcns.repository.FcmDeviceRepository;
import com.lgcns.service.FcmService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
public class NotificationIntegrationTest {
    @MockitoBean protected FcmService fcmService;
    @MockitoBean protected FirebaseMessaging firebaseMessaging;
    @MockitoBean protected FirebaseApp firebaseApp;
    @MockitoBean protected FcmDeviceRepository fcmDeviceRepository;
}
