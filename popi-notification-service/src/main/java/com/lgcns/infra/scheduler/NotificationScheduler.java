package com.lgcns.infra.scheduler;

import com.lgcns.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void sendReservationNotification() {
        notificationService.sendReservationNotification();
    }
}
