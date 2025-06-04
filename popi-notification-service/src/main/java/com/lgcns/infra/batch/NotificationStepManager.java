package com.lgcns.infra.batch;

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

    private static final String NOTIFICATION_ITEM_READER = "notificationItemReader";
    private static final String NOTIFICATION_ITEM_PROCESSOR = "notificationItemProcessor";
    private static final String NOTIFICATION_ITEM_WRITER = "notificationItemWriter";

    @Bean(name = NOTIFICATION_ITEM_READER)
    @StepScope
    public ItemReader notificationItemReader() {}

    @Bean(name = NOTIFICATION_ITEM_PROCESSOR)
    @StepScope
    public ItemProcessor notificationItemProcessor() {}

    @Bean(name = NOTIFICATION_ITEM_WRITER)
    @StepScope
    public ItemWriter notificationItemWriter() {}
}
