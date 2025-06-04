package com.lgcns.service;

import com.lgcns.dto.request.FcmRequest;
import com.lgcns.infra.firebase.FcmSender;
import com.lgcns.repository.FcmDeviceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final FcmDeviceRepository fcmDeviceRepository;
    private final FcmSender fcmSender;
    private final RedisTemplate<String, Long> redisTemplate;

    @Override
    public void sendNotification(List<FcmRequest> fcmRequestList) {
        fcmRequestList.forEach(fcmSender::sendFcm);
    }
}
