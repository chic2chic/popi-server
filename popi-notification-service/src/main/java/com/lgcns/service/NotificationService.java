package com.lgcns.service;

import java.util.List;

public interface NotificationService {
    List<Long> findTargetMemberIds();

    void sendNotification(List<String> fcmTokens);

    String findFcmToken(Long memberId);

    void saveFcmToken(String memberId, String fcmToken);
}
