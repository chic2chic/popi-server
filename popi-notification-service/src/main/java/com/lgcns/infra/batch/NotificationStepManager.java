package com.lgcns.infra.batch;

import com.lgcns.service.NotificationService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class NotificationStepManager {

    private static final String SEND_NOTIFICATION_ITEM_READER = "sendNotificationItemReader";
    private static final String SEND_NOTIFICATION_ITEM_WRITER = "sendNotificationItemWriter";

    private final NotificationService notificationService;

    @Bean(name = SEND_NOTIFICATION_ITEM_READER)
    @StepScope
    public ItemReader<Long> sendNotificationItemReader() {
        return null;
    }

    @Bean(name = SEND_NOTIFICATION_ITEM_WRITER)
    @StepScope
    public ItemWriter<Long> sendNotificationItemWriter() {
        return chunk -> {
            List<Long> memberIds = new ArrayList<>(chunk.getItems());
            notificationService.sendNotification(memberIds);
        };
    }
}
