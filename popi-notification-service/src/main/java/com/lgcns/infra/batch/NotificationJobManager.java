package com.lgcns.infra.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class NotificationJobManager {

    public static final Integer CHUNK_SIZE = 100;
    public static final Integer TASK_POOL_SIZE = 2;
    public static final String NOTIFICATION_JOB = "notificationJob";
    public static final String SEND_NOTIFICATION_STEP = "sendNotificationStep";
    public static final String NOTIFICATION_TASK_EXECUTOR = "notificationTaskExecutor";

    @Bean(name = NOTIFICATION_JOB)
    public Job notificationJob(JobRepository jobRepository, Step notificationStep) {
        return new JobBuilder(NOTIFICATION_JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(notificationStep)
                .build();
    }

    @Bean(name = SEND_NOTIFICATION_STEP)
    @JobScope
    public Step notificationStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader sendNotificationReader,
            ItemProcessor sendNotificationProcessor,
            ItemWriter sendNotificationWriter,
            @Qualifier(NOTIFICATION_TASK_EXECUTOR) TaskExecutor taskExecutor) {
        return new StepBuilder(SEND_NOTIFICATION_STEP, jobRepository)
                .chunk(CHUNK_SIZE, transactionManager)
                .reader(sendNotificationReader)
                .processor(sendNotificationProcessor)
                .writer(sendNotificationWriter)
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean(name = NOTIFICATION_TASK_EXECUTOR)
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(TASK_POOL_SIZE);
        taskExecutor.setMaxPoolSize(TASK_POOL_SIZE * 2);
        taskExecutor.setThreadNamePrefix("async-thread");
        return taskExecutor;
    }
}
