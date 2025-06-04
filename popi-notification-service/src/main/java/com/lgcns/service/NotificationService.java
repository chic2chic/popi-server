package com.lgcns.service;

import com.lgcns.dto.request.FcmRequest;
import java.util.List;

public interface NotificationService {

    void sendNotification(List<FcmRequest> fcmRequestList);
}
