package com.lgcns.service.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.NotificationErrorCode;
import com.lgcns.service.FcmService;
import com.lgcns.service.NotificationServiceImpl;
import java.util.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceUnitTest {

    @InjectMocks private NotificationServiceImpl notificationService;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private FcmService fcmService;
    @Mock private ValueOperations<String, String> redisValueOperations;
    @Mock private ZSetOperations<String, String> redisZSetOperations;

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

            Set<String> mockZSetMembers = new LinkedHashSet<>(List.of(member1, member2));

            given(redisTemplate.opsForZSet()).willReturn(redisZSetOperations);
            given(redisZSetOperations.rangeByScore(anyString(), anyDouble(), anyDouble()))
                    .willReturn(mockZSetMembers);

            // when
            List<Long> result = notificationService.findTargetMemberIds();

            // then
            verify(redisZSetOperations, times(1))
                    .rangeByScore(eq(ZSET_KEY), anyDouble(), anyDouble());
            verify(redisZSetOperations, times(1))
                    .remove(eq(ZSET_KEY), eq(mockZSetMembers.toArray()));
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).hasSize(2),
                    () -> assertThat(result).containsExactly(memberId1, memberId2),
                    () -> assertThat(redisTemplate.opsForZSet().size(ZSET_KEY)).isEqualTo(0));
        }

        @Test
        void 예약이_한_시간_남은_사용자가_없으면_빈_리스트를_반환한다() {
            // given
            Set<String> mockZSetMembers = Collections.emptySet();

            given(redisTemplate.opsForZSet()).willReturn(redisZSetOperations);
            given(redisZSetOperations.rangeByScore(anyString(), anyDouble(), anyDouble()))
                    .willReturn(mockZSetMembers);

            // when
            List<Long> result = notificationService.findTargetMemberIds();

            // then
            verify(redisZSetOperations, times(1))
                    .rangeByScore(eq(ZSET_KEY), anyDouble(), anyDouble());
            assertAll(() -> assertThat(result).isNotNull(), () -> assertThat(result).hasSize(0));
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

            given(redisTemplate.opsForValue()).willReturn(redisValueOperations);
            given(redisTemplate.opsForValue().get(key)).willReturn(token);

            // when
            String result = notificationService.findFcmToken(memberId);

            // then
            verify(redisValueOperations, times(1)).get(eq(key));
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).isEqualTo(token));
        }

        @Test
        void Redis_접근_중_에러가_발생하면_예외를_반환한다() {
            // given
            Long memberId = 1L;
            String key = memberFcmKey(memberId);

            given(redisTemplate.opsForValue()).willReturn(redisValueOperations);
            given(redisValueOperations.get(key))
                    .willThrow(new DataAccessException("Redis error") {});

            // when & then
            assertThatThrownBy(() -> notificationService.findFcmToken(memberId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(NotificationErrorCode.REDIS_ACCESS_FAILED.getMessage());
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
            verify(fcmService, times(2)).sendMessageSync(anyString());
        }

        @Test
        void FCM_토큰이_없으면_전송하지_않는다() {
            // given
            List<String> tokens = new ArrayList<>();

            // when
            notificationService.sendNotification(tokens);

            // then
            verify(fcmService, never()).sendMessageSync(anyString());
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

            given(redisTemplate.opsForValue()).willReturn(redisValueOperations);

            // when
            notificationService.saveFcmToken(memberId, token);

            // then
            verify(redisValueOperations, times(1)).get(eq(key));
            verify(redisTemplate, never()).delete(eq(key));
            verify(redisValueOperations, times(1)).set(eq(key), anyString());
        }

        @Test
        void Redis에_동일한_토큰이_존재하면_예외를_반환한다() {
            // given
            String memberId = "1";
            String token = "token";
            String key = "memberId: " + memberId;

            given(redisTemplate.opsForValue()).willReturn(redisValueOperations);
            given(redisValueOperations.get(eq(key))).willReturn(token);

            // when & then
            assertThatThrownBy(() -> notificationService.saveFcmToken(memberId, token))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(NotificationErrorCode.FCM_TOKEN_DUPLICATED.getMessage());
        }

        @Test
        void 동일한_사용자에_대해_새로운_토큰이_포함되면_토큰을_갱신한다() {
            // given
            String memberId = "1";
            String token1 = "token1";
            String token2 = "token2";
            String key = "memberId: " + memberId;

            given(redisTemplate.opsForValue()).willReturn(redisValueOperations);
            given(redisValueOperations.get(eq(key))).willReturn(token1);

            // when
            notificationService.saveFcmToken(memberId, token2);

            // then
            verify(redisValueOperations, times(1)).get(eq(key));
            verify(redisTemplate, times(1)).delete(eq(key));
            verify(redisValueOperations, times(1)).set(eq(key), anyString());
        }

        @Test
        void Redis_접근_중_에러가_발생하면_예외를_반환한다() {
            // given
            String memberId = "1";
            String key = "memberId: " + memberId;
            String token = "token";

            given(redisTemplate.opsForValue()).willReturn(redisValueOperations);
            given(redisValueOperations.get(key))
                    .willThrow(new DataAccessException("Redis error") {});

            // when & then
            assertThatThrownBy(() -> notificationService.saveFcmToken(memberId, token))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(NotificationErrorCode.REDIS_ACCESS_FAILED.getMessage());
        }
    }

    private String memberFcmKey(Long memberId) {
        return "memberId: " + memberId;
    }
}
