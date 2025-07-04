package com.lgcns.service;

import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.NotificationErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final String ZSET_KEY = "reservation:notifications";

    private final FcmService fcmService;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public List<Long> findTargetMemberIds() {
        LocalDateTime now = LocalDateTime.now();
        long epochTime = now.atZone(ZoneId.of("Asia/Seoul")).toEpochSecond();

        Set<String> zSetMembers =
                Optional.ofNullable(
                                redisTemplate
                                        .opsForZSet()
                                        .rangeByScore(
                                                ZSET_KEY,
                                                (double) (epochTime - 10),
                                                (double) (epochTime + 10)))
                        .orElse(Collections.emptySet());

        List<Long> memberIds =
                zSetMembers.stream()
                        .map(member -> member.split("\\|"))
                        .filter(parts -> parts.length == 2)
                        .map(parts -> parts[1])
                        .map(Long::parseLong)
                        .toList();

        if (!zSetMembers.isEmpty()) {
            redisTemplate.opsForZSet().remove(ZSET_KEY, zSetMembers.toArray());
        }

        return memberIds;
    }

    @Override
    public String findFcmToken(Long memberId) {
        try {
            String key = "memberId: " + memberId;
            return redisTemplate.opsForValue().get(key);
        } catch (DataAccessException e) {
            throw new CustomException(NotificationErrorCode.REDIS_ACCESS_FAILED);
        }
    }

    @Override
    public void sendNotification(List<String> fcmTokens) {
        fcmTokens.forEach(fcmService::sendMessageSync);
    }

    @Override
    public void saveFcmToken(String memberId, String fcmToken) {

        try {
            String key = "memberId: " + memberId;
            String existingToken = redisTemplate.opsForValue().get(key);

            if (existingToken != null) {
                if (existingToken.equals(fcmToken)) {
                    throw new CustomException(NotificationErrorCode.FCM_TOKEN_DUPLICATED);
                }
                redisTemplate.delete(key);
            }

            redisTemplate.opsForValue().set(key, fcmToken);

        } catch (DataAccessException e) {
            throw new CustomException(NotificationErrorCode.REDIS_ACCESS_FAILED);
        }
    }
}
