package com.lgcns.service;

import java.util.List;

public interface NotificationService {
    List<Long> findTargetMemberIds();

    void sendNotification(List<Long> memberIds);

    void saveFcmToken(Long memberId, String fcmToken);
}
