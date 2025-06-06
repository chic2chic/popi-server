package com.lgcns.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.lgcns.NotificationIntegrationTest;
import com.lgcns.dto.request.FcmRequest;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.FirebaseErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBatchTest
public class NotificationJobTest extends NotificationIntegrationTest {

    private static final String NOTIFICATION_JOB = "notificationJob";
    private static final String ZSET_KEY = "reservation:notifications";

    @Autowired private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired RedisTemplate<String, String> redisTemplate;

    @Autowired
    @Qualifier(NOTIFICATION_JOB)
    Job notificationJob;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(notificationJob);
    }

    @Nested
    class notification_job이_실행될_때 {

        @Test
        void 예약이_한_시간_남은_사용자가_존재하고_알림이_정상적으로_전송되면_실행에_성공한다() throws Exception {
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

            List<String> tokens = List.of("token1", "token2");
            when(fcmDeviceRepository.findFcmTokensByMemberIds(List.of(memberId1, memberId2)))
                    .thenReturn(tokens);

            JobParameters jobParameters = buildJobParameters();

            // when
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
            StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();

            // then
            assertAll(
                    () -> assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED),
                    () -> assertThat(stepExecution.getReadCount()).isEqualTo(2),
                    () -> assertThat(stepExecution.getWriteCount()).isEqualTo(2),
                    () -> assertThat(stepExecution.getSkipCount()).isEqualTo(0));

            redisTemplate.opsForZSet().remove(ZSET_KEY, member1);
            redisTemplate.opsForZSet().remove(ZSET_KEY, member2);
        }

        @Test
        void 예약이_한_시간_남은_사용자가_존재하지_않아도_실행에_성공한다() throws Exception {
            // given
            Long memberId = 1L;
            Long memberReservationId = 1L;
            String member = memberId + "|" + memberReservationId;

            LocalDateTime now = LocalDateTime.now();
            long epochTime = now.minusHours(1).atZone(ZoneId.of("Asia/Seoul")).toEpochSecond();

            redisTemplate.opsForZSet().add(ZSET_KEY, member, epochTime);

            JobParameters jobParameters = buildJobParameters();

            // when
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
            StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();

            // then
            assertAll(
                    () -> assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED),
                    () -> assertThat(stepExecution.getReadCount()).isEqualTo(0),
                    () -> assertThat(stepExecution.getWriteCount()).isEqualTo(0),
                    () -> assertThat(stepExecution.getSkipCount()).isEqualTo(0));

            redisTemplate.opsForZSet().remove(ZSET_KEY, member);
        }

        @Test
        void 알림_전송_중_예외가_발생한_경우_Step은_스킵되고_Job은_실행에_성공한다() throws Exception {
            // given
            Long memberId = 1L;
            Long memberReservationId = 1L;
            String member = memberId + "|" + memberReservationId;

            LocalDateTime now = LocalDateTime.now();
            long epochTime = now.atZone(ZoneId.of("Asia/Seoul")).toEpochSecond();

            redisTemplate.opsForZSet().add(ZSET_KEY, member, epochTime);

            List<String> tokens = List.of("token");
            when(fcmDeviceRepository.findFcmTokensByMemberIds(List.of(memberId)))
                    .thenReturn(tokens);

            FcmRequest fcmRequest = FcmRequest.of("token");
            doThrow(new CustomException(FirebaseErrorCode.FCM_SEND_FAILED))
                    .when(fcmService)
                    .sendMessageSync(fcmRequest);

            JobParameters jobParameters = buildJobParameters();

            // when
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
            StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();

            // then
            assertAll(
                    () -> assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED),
                    () -> assertThat(stepExecution.getReadCount()).isEqualTo(1),
                    () -> assertThat(stepExecution.getWriteCount()).isEqualTo(0),
                    () -> assertThat(stepExecution.getSkipCount()).isEqualTo(1));

            redisTemplate.opsForZSet().remove(ZSET_KEY, member);
        }
    }

    private JobParameters buildJobParameters() {
        return new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
    }
}
