package com.lgcns.infra.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FcmConfig {

    @Value("${fcm.certification}")
    private String fcmCertification;

    @PostConstruct
    public void init() {
        try {
            ByteArrayInputStream inputStream =
                    new ByteArrayInputStream(fcmCertification.getBytes(StandardCharsets.UTF_8));

            FirebaseOptions options =
                    FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(inputStream))
                            .build();

            FirebaseApp.initializeApp(options);
        } catch (Exception e) {
            log.error("FCM initializing Exception; {}", e.getStackTrace()[0]);
        }
    }
}
