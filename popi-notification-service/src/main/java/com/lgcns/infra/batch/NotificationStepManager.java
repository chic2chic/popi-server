package com.lgcns.infra.batch;

import com.lgcns.dto.request.FcmRequest;
import com.lgcns.service.NotificationService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class NotificationStepManager {

    private static final String SEND_NOTIFICATION_ITEM_READER = "sendNotificationItemReader";
    private static final String SEND_NOTIFICATION_ITEM_PROCESSOR = "sendNotificationItemProcessor";
    private static final String SEND_NOTIFICATION_ITEM_WRITER = "sendNotificationItemWriter";

    private final NotificationService notificationService;

    @Bean(name = SEND_NOTIFICATION_ITEM_READER)
    @StepScope
    public ItemReader sendNotificationItemReader() {
        return null;
    }

    @Bean(name = SEND_NOTIFICATION_ITEM_PROCESSOR)
    @StepScope
    public ItemProcessor sendNotificationItemProcessor() {
        return null;
    }

    @Bean(name = SEND_NOTIFICATION_ITEM_WRITER)
    @StepScope
    public ItemWriter<FcmRequest> sendNotificationItemWriter() {
        return chunk -> {
            List<FcmRequest> fcmRequestList = new ArrayList<>(chunk.getItems());
            notificationService.sendNotification(fcmRequestList);
        };
    }
}
