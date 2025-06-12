package com.lgcns.service.integration;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.NotificationErrorCode;
import com.lgcns.service.NotificationService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

public class NotificationServiceIntegrationTest extends NotificationIntegrationTest {

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

            String member1 = memberId1 + "|" + memberReservationId1;
            String member2 = memberId2 + "|" + memberReservationId2;

            LocalDateTime now = LocalDateTime.now();
            long epochTime = now.atZone(ZoneId.of("Asia/Seoul")).toEpochSecond();

            redisTemplate.opsForZSet().add(ZSET_KEY, member1, epochTime);
            redisTemplate.opsForZSet().add(ZSET_KEY, member2, epochTime);

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
            Long memberId = 1L;
            Long memberReservationId = 1L;

            String member = memberId + "|" + memberReservationId;

            LocalDateTime now = LocalDateTime.now();
            long epochTime = now.minusHours(1).atZone(ZoneId.of("Asia/Seoul")).toEpochSecond();

            redisTemplate.opsForZSet().add(ZSET_KEY, member, epochTime);

            // when
            List<Long> result = notificationService.findTargetMemberIds();

            // then
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).hasSize(0),
                    () -> assertThat(redisTemplate.opsForZSet().size(ZSET_KEY)).isEqualTo(1));

            redisTemplate.opsForZSet().remove(ZSET_KEY, member);
        }
    }

    @Nested
    class FCM_토큰을_조회할_때 {

        @Test
        void Redis에_member_id에_대응하는_FCM_토큰이_존재하면_조회에_성공한다() {
            // given
            Long memberId = 1L;
            String key = memberFcmKey(memberId);
            String token = "token";

            redisTemplate.opsForValue().set(key, token);

            // when
            String result = notificationService.findFcmToken(memberId);

            // then
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).isEqualTo(token));
        }
    }

    @Nested
    class 사용자에게_알림을_전송할_때 {

        @Test
        void FCM_토큰이_존재하면_전송에_성공한다() {
            // given
            List<String> tokens = List.of("token1", "token2");

            // when
            notificationService.sendNotification(tokens);

            // then
            assertAll(() -> verify(fcmService, times(2)).sendMessageSync(anyString()));
        }

        @Test
        void FCM_토큰이_없으면_전송하지_않는다() {
            // given
            List<String> tokens = new ArrayList<>();

            // when
            notificationService.sendNotification(tokens);

            // then
            assertAll(() -> verify(fcmService, never()).sendMessageSync(anyString()));
        }

        @Test
        void 알림_전송_과정에서_오류가_발생하면_예외를_반환한다() {
            // given
            List<String> tokens = List.of("token");

            doThrow(new CustomException(NotificationErrorCode.FCM_SEND_FAILED))
                    .when(fcmService)
                    .sendMessageSync(tokens.get(0));

            // when & then
            assertThatThrownBy(() -> notificationService.sendNotification(tokens))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(NotificationErrorCode.FCM_SEND_FAILED.getMessage());
        }
    }

    @Nested
    class FCM_토큰을_저장할_때 {

        @Test
        void 유효한_토큰이_포함되고_Redis에_동일한_토큰이_존재하지_않으면_저장에_성공한다() {
            // given
            String memberId = "1";
            String token = "token";
            String key = "memberId: " + memberId;

            // when
            notificationService.saveFcmToken(memberId, token);

            // then
            assertAll(
                    () -> assertThat(redisTemplate.hasKey(key)).isTrue(),
                    () -> assertThat(redisTemplate.opsForValue().get(key)).isEqualTo(token));
            redisTemplate.delete(key);
        }

        @Test
        void Redis에_동일한_토큰이_존재하면_예외를_반환한다() {
            // given
            String memberId = "1";
            String token = "token";
            String key = "memberId: " + memberId;

            redisTemplate.opsForValue().set(key, token);

            // when & then
            assertThatThrownBy(() -> notificationService.saveFcmToken(memberId, token))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(NotificationErrorCode.FCM_TOKEN_DUPLICATED.getMessage());
            redisTemplate.delete(key);
        }

        @Test
        void 동일한_사용자에_대해_새로운_토큰이_포함되면_토큰을_갱신한다() {
            // given
            String memberId = "1";
            String token1 = "token1";
            String token2 = "token2";
            String key = "memberId: " + memberId;

            redisTemplate.opsForValue().set(key, token1);

            // when
            notificationService.saveFcmToken(memberId, token2);

            // then
            assertAll(
                    () -> assertThat(redisTemplate.hasKey(key)).isTrue(),
                    () -> assertThat(redisTemplate.opsForValue().get(key)).isEqualTo(token2));
            redisTemplate.delete(key);
        }
    }

    private String memberFcmKey(Long memberId) {
        return "memberId: " + memberId;
    }
}
