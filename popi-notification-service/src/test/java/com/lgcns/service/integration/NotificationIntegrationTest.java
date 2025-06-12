package com.lgcns.service.integration;

import com.lgcns.service.FcmService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
public class NotificationIntegrationTest {
    @MockitoBean protected FcmService fcmService;
}
