package com.lgcns.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.lgcns.NotificationIntegrationTest;
import com.lgcns.dto.request.FcmRequest;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.FirebaseErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

public class NotificationServiceTest extends NotificationIntegrationTest {

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
    class 사용자에게_알림을_전송할_때 {

        @Test
        void 알림을_전송할_사용자의_fcm_token이_있으면_전송에_성공한다() {
            // given
            List<Long> memberIds = List.of(1L, 2L);
            List<String> tokens = List.of("token1", "token2");

            when(fcmDeviceRepository.findFcmTokensByMemberIds(memberIds)).thenReturn(tokens);

            // when
            notificationService.sendNotification(memberIds);

            // then
            assertAll(
                    () -> verify(fcmDeviceRepository, times(1)).findFcmTokensByMemberIds(memberIds),
                    () -> verify(fcmService, times(2)).sendMessageSync(any(FcmRequest.class)));
        }

        @Test
        void 알림을_전송할_사용자가_없으면_전송하지_않는다() {
            // given
            List<Long> memberIds = new ArrayList<>();

            // when
            notificationService.sendNotification(memberIds);

            // then
            assertAll(
                    () -> verify(fcmDeviceRepository, times(1)).findFcmTokensByMemberIds(memberIds),
                    () -> verify(fcmService, never()).sendMessageSync(any(FcmRequest.class)));
        }

        @Test
        void 알림_전송_과정에서_오류가_발생하면_예외를_반환한다() {
            // given
            List<Long> memberIds = List.of(1L);
            List<String> tokens = List.of("token");

            when(fcmDeviceRepository.findFcmTokensByMemberIds(memberIds)).thenReturn(tokens);

            FcmRequest fcmRequest = FcmRequest.of("token");

            doThrow(new CustomException(FirebaseErrorCode.FCM_SEND_FAILED))
                    .when(fcmService)
                    .sendMessageSync(fcmRequest);

            // when & then
            assertThatThrownBy(() -> notificationService.sendNotification(memberIds))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(FirebaseErrorCode.FCM_SEND_FAILED.getMessage());
        }
    }
}
