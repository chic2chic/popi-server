package com.lgcns.infra.batch;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationScheduler {

    public static final String NOTIFICATION_JOB = "notificationJob";

    private final Job notificationJob;
    private final JobLauncher jobLauncher;

    public NotificationScheduler(@Qualifier(NOTIFICATION_JOB) Job job, JobLauncher jobLauncher) {
        this.notificationJob = job;
        this.jobLauncher = jobLauncher;
    }

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void sendReservationNotification()
            throws JobInstanceAlreadyCompleteException,
                    JobExecutionAlreadyRunningException,
                    JobParametersInvalidException,
                    JobRestartException {
        String dateHour = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"));

        JobParameters jobParameters =
                new JobParametersBuilder().addString("dateHour", dateHour).toJobParameters();

        jobLauncher.run(notificationJob, jobParameters);
    }
}
