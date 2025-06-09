package com.lgcns.service.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.lgcns.exception.MemberReservationErrorCode.RESERVATION_FAILED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.domain.MemberReservation;
import com.lgcns.repository.MemberReservationRepository;
import com.lgcns.service.MemberReservationService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class MemberReservationMockTest extends WireMockIntegrationTest {

    @MockitoBean private MemberReservationRepository memberReservationRepository;
    @Autowired private MemberReservationService memberReservationService;

    @Autowired
    @Qualifier("reservationRedisTemplate")
    private RedisTemplate<String, Long> reservationRedisTemplate;

    @Autowired
    @Qualifier("notificationRedisTemplate")
    private RedisTemplate<String, String> notificationRedisTemplate;

    @Autowired private DatabaseCleaner databaseCleaner;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String memberId = "1";
    private final Long reservationId = 1L;

    @BeforeEach
    void cleanDatabase() {
        databaseCleaner.execute();
    }

    @Nested
    class 회원이_예약_생성_할_때 {
        @Test
        void 예약_저장_중_예외_발생시_복구_및_예외_반환() throws JsonProcessingException {
            // given
            reservationRedisTemplate.opsForValue().set(reservationId.toString(), 10L);
            stubForFindMemberInternalInfo(
                    memberId,
                    200,
                    objectMapper.writeValueAsString(
                            Map.of("memberId", memberId, "role", "USER", "status", "NORMAL")));
            given(memberReservationRepository.save(ArgumentMatchers.any(MemberReservation.class)))
                    .willThrow(new RuntimeException("DB error"));

            // when & then
            assertThatThrownBy(
                            () ->
                                    memberReservationService.createMemberReservation(
                                            memberId, reservationId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(RESERVATION_FAILED.getMessage());

            assertThat(reservationRedisTemplate.opsForValue().get(reservationId.toString()))
                    .isEqualTo(10L);
            reservationRedisTemplate.delete(reservationId.toString());
        }
    }

    private void stubForFindMemberInternalInfo(String memberId, int status, String body) {
        try {
            wireMockServer.stubFor(
                    get(urlPathEqualTo("/internal/" + memberId))
                            .willReturn(
                                    aResponse()
                                            .withStatus(status)
                                            .withHeader(
                                                    "Content-Type",
                                                    MediaType.APPLICATION_JSON_VALUE)
                                            .withBody(body)));
        } catch (Exception e) {
            throw new RuntimeException("직렬화 실패", e);
        }
    }
}
