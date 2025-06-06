package com.lgcns.service;

import com.lgcns.dto.request.FcmRequest;
import com.lgcns.repository.FcmDeviceRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final String ZSET_KEY = "reservation:notifications";

    private final FcmService fcmService;
    private final FcmDeviceRepository fcmDeviceRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public List<Long> findTargetMemberIds() {
        LocalDateTime now = LocalDateTime.now();
        long epochTime = now.atZone(ZoneId.of("Asia/Seoul")).toEpochSecond();

        Set<String> zSetMembers =
                Optional.ofNullable(
                                redisTemplate
                                        .opsForZSet()
                                        .rangeByScore(ZSET_KEY, epochTime - 10, epochTime + 10))
                        .orElse(Collections.emptySet());

        List<Long> memberIds =
                zSetMembers.stream()
                        .map(member -> member.split("\\|"))
                        .filter(parts -> parts.length == 2)
                        .map(parts -> parts[1])
                        .map(Long::parseLong)
                        .collect(Collectors.toList());

        if (!zSetMembers.isEmpty()) {
            redisTemplate.opsForZSet().remove(ZSET_KEY, zSetMembers.toArray());
        }

        return memberIds;
    }

    @Override
    public void sendNotification(List<Long> memberIds) {
        List<String> fcmTokens = fcmDeviceRepository.findFcmTokensByMemberIds(memberIds);

        fcmTokens.forEach(
                fcmToken -> {
                    FcmRequest fcmRequest = FcmRequest.of(fcmToken);
                    fcmService.sendMessageSync(fcmRequest);
                });
    }
}
