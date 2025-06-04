package com.lgcns.service;

import com.lgcns.dto.request.FcmRequest;
import com.lgcns.infra.firebase.FcmSender;
import com.lgcns.repository.FcmDeviceRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final String ZSET_KEY = "reservation:notifications";

    private final FcmDeviceRepository fcmDeviceRepository;
    private final FcmSender fcmSender;
    private final RedisTemplate<String, Long> redisTemplate;

    @Override
    public List<Long> findTargetMemberIds() {
        LocalDateTime now = LocalDateTime.now();
        long epochTime = now.atZone(ZoneId.of("ASIA/SEOUL")).toEpochSecond();

        Set<Long> memberIds =
                redisTemplate.opsForZSet().rangeByScore(ZSET_KEY, epochTime, epochTime);

        return new ArrayList<>(Optional.ofNullable(memberIds).orElse(Collections.emptySet()));
    }

    @Override
    public void sendNotification(List<Long> memberIds) {
        List<String> fcmTokens = fcmDeviceRepository.findFcmTokensByMemberIds(memberIds);

        fcmTokens.forEach(
                fcmToken -> {
                    FcmRequest fcmRequest = FcmRequest.of("title", "body", fcmToken);
                    fcmSender.sendFcm(fcmRequest);
                });
    }
}
