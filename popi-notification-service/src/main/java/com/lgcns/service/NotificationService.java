package com.lgcns.service;

import java.util.List;

public interface NotificationService {
    void sendNotification(List<Long> memberIds);
}
