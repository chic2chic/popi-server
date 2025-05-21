package com.lgcns.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.FirebaseErrorCode;
import com.lgcns.infra.firebase.FirebaseProperties;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class FirebaseConfig {

    private final FirebaseProperties firebaseProperties;

    @Bean
    public FirebaseApp firebaseApp() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        FirebaseOptions options =
                new FirebaseOptions.Builder().setCredentials(getCredentials()).build();
        return FirebaseApp.initializeApp(options);
    }

    private GoogleCredentials getCredentials() {
        String credentialsJson = firebaseProperties.credentialsJson();

        try {
            InputStream serviceAccount =
                    new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
            return GoogleCredentials.fromStream(serviceAccount);
        } catch (IOException e) {
            throw new CustomException(FirebaseErrorCode.FCM_FILE_CONVERSION_FAILED);
        }
    }
}
