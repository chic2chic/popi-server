package com.lgcns.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import com.google.firebase.FirebaseApp;
import com.lgcns.NotificationIntegrationTest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class NotificationServiceTest extends NotificationIntegrationTest {

    // @MockitoBean private FcmSender fcmSender;
    // @MockitoBean private FirebaseMessaging firebaseMessaging;
    @MockitoBean private FirebaseApp firebaseApp;

    @Autowired private NotificationService notificationService;
    @Autowired private RedisTemplate<String, String> redisTemplate;

    private static final String ZSET_KEY = "reservation:notifications";

    @Nested
    class 알림을_보낼_사용자_ID_리스트를_조회할_때 {

        @Test
        void 예약이_한_시간_남은_사용자가_존재하면_조회에_성공한다() {
            // given
            Long memberId1 = 1L;
            Long memberId2 = 2L;
            Long memberReservationId1 = 1L;
            Long memberReservationId2 = 2L;

            LocalDateTime now = LocalDateTime.now();
            long epochTime = now.atZone(ZoneId.of("Asia/Seoul")).toEpochSecond();

            redisTemplate
                    .opsForZSet()
                    .add(ZSET_KEY, memberId1 + "|" + memberReservationId1, epochTime);
            redisTemplate
                    .opsForZSet()
                    .add(ZSET_KEY, memberId2 + "|" + memberReservationId2, epochTime);

            // when
            List<Long> result = notificationService.findTargetMemberIds();

            // then
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).hasSize(2),
                    () -> assertThat(result.get(0)).isEqualTo(memberId1),
                    () -> assertThat(result.get(1)).isEqualTo(memberId2),
                    () -> assertThat(redisTemplate.opsForZSet().size(ZSET_KEY)).isEqualTo(0));
        }

        @Test
        void 예약이_한_시간_남은_사용자가_없으면_빈_리스트를_반환한다() {
            // given
            Long memberId1 = 1L;
            Long memberId2 = 2L;
            Long memberReservationId1 = 1L;
            Long memberReservationId2 = 2L;

            LocalDateTime now = LocalDateTime.now();
            long epochTime = now.minusHours(1).atZone(ZoneId.of("Asia/Seoul")).toEpochSecond();

            redisTemplate
                    .opsForZSet()
                    .add(ZSET_KEY, memberId1 + "|" + memberReservationId1, epochTime);
            redisTemplate
                    .opsForZSet()
                    .add(ZSET_KEY, memberId2 + "|" + memberReservationId2, epochTime);

            // when
            List<Long> result = notificationService.findTargetMemberIds();

            // then
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).hasSize(0),
                    () -> assertThat(redisTemplate.opsForZSet().size(ZSET_KEY)).isEqualTo(2));
        }
    }
}
