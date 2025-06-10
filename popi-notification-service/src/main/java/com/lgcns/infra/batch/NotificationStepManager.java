package com.lgcns.infra.batch;

import com.lgcns.service.NotificationService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.*;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class NotificationStepManager {

    public static final String SEND_NOTIFICATION_ITEM_READER = "sendNotificationItemReader";
    public static final String SEND_NOTIFICATION_ITEM_PROCESSOR = "sendNotificationProcessor";
    public static final String SEND_NOTIFICATION_ITEM_WRITER = "sendNotificationItemWriter";

    private final NotificationService notificationService;

    @Bean(name = SEND_NOTIFICATION_ITEM_READER)
    @StepScope
    public ItemReader<Long> sendNotificationItemReader() {
        ListItemReader<Long> delegate =
                new ListItemReader<>(notificationService.findTargetMemberIds());

        return new ItemReader<>() {
            @Override
            public synchronized Long read() {
                return delegate.read();
            }
        };
    }

    @Bean(name = SEND_NOTIFICATION_ITEM_PROCESSOR)
    @StepScope
    public ItemProcessor<Long, String> sendNotificationItemProcessor() {
        return notificationService::findFcmToken;
    }

    @Bean(name = SEND_NOTIFICATION_ITEM_WRITER)
    @StepScope
    public ItemWriter<String> sendNotificationItemWriter() {
        return chunk -> {
            List<String> fcmTokens = new ArrayList<>(chunk.getItems());
            notificationService.sendNotification(fcmTokens);
        };
    }
}
