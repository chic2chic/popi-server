package com.lgcns.service.unit;

import static com.lgcns.domain.MemberReservationStatus.RESERVED;
import static com.lgcns.exception.MemberReservationErrorCode.RESERVATION_FAILED;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.client.managerClient.ManagerServiceClient;
import com.lgcns.client.managerClient.dto.response.*;
import com.lgcns.client.memberClient.MemberServiceClient;
import com.lgcns.domain.MemberReservation;
import com.lgcns.dto.request.QrEntranceInfoRequest;
import com.lgcns.dto.request.SurveyChoiceRequest;
import com.lgcns.dto.response.*;
import com.lgcns.enums.MemberAge;
import com.lgcns.enums.MemberGender;
import com.lgcns.enums.MemberRole;
import com.lgcns.enums.MemberStatus;
import com.lgcns.error.exception.CustomException;
import com.lgcns.event.dto.MemberReservationNotificationEvent;
import com.lgcns.event.dto.MemberReservationUpdateEvent;
import com.lgcns.exception.MemberReservationErrorCode;
import com.lgcns.kafka.message.MemberEnteredMessage;
import com.lgcns.kafka.producer.MemberAnswerProducer;
import com.lgcns.repository.MemberReservationRepository;
import com.lgcns.service.MemberReservationServiceImpl;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class MemberReservationServiceUnitTest {

    @InjectMocks MemberReservationServiceImpl memberReservationService;

    @Mock MemberReservationRepository memberReservationRepository;

    @Mock ManagerServiceClient managerServiceClient;
    @Mock MemberServiceClient memberServiceClient;

    @Mock private RedisTemplate<String, Long> reservationRedisTemplate;
    @Mock private ValueOperations<String, Long> reservationRedisValueOperations;
    @Mock private RedisTemplate<String, String> notificationRedisTemplate;

    @Mock private ApplicationEventPublisher eventPublisher;

    @Mock MemberAnswerProducer memberAnswerProducer;

    private final String memberId = "1";
    private final Long popupId = 1L;
    private final Long reservationId = 1L;
    private final Long memberReservationId = 1L;

    @Nested
    class 예약_가능한_날짜를_조회할_때 {

        @Test
        void 날짜에_문자가_포함되면_예외가_발생한다() {
            // given
            String invalidDate = "2025-July";

            // when & then
            assertThatThrownBy(
                            () -> memberReservationService.findAvailableDate(popupId, invalidDate))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode", MemberReservationErrorCode.INVALID_DATE_FORMAT);
        }

        @Test
        void 날짜_형식이_yyyy_MM_아니면_예외가_발생한다() {
            // given
            String invalidDate = "2025-5";

            // when & then
            assertThatThrownBy(
                            () -> memberReservationService.findAvailableDate(popupId, invalidDate))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode", MemberReservationErrorCode.INVALID_DATE_FORMAT);
        }

        @Test
        void 월이_1부터_12가_아니면_예외가_발생한다() {
            // given
            String invalidDate = "2025-13";

            // when & then
            assertThatThrownBy(
                            () -> memberReservationService.findAvailableDate(popupId, invalidDate))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode", MemberReservationErrorCode.INVALID_DATE_FORMAT);
        }

        @Test
        void 예약이_없는_경우_모든_시간이_예약_가능하다() {
            // given
            String date = "2025-06";
            LocalDate targetDate = LocalDate.of(2025, 6, 30);

            givenThisMonthReservation(date);
            givenEmptyReservedTimeSlots(targetDate);

            // when
            AvailableDateResponse response =
                    memberReservationService.findAvailableDate(popupId, date);

            // then
            assertPopupDate(response);

            for (ReservableDate reservableDate : response.reservableDate()) {
                assertThat(reservableDate.isReservable()).isTrue();

                for (ReservableTime reservableTime : reservableDate.timeSlots()) {
                    assertThat(reservableTime.isPossible()).isTrue();
                }
            }

            verify(memberReservationRepository)
                    .findDailyReservationCount(
                            eq(popupId),
                            eq(LocalDate.of(2025, 6, 30)),
                            eq(LocalDate.of(2025, 7, 2)),
                            eq("2025-06"));
        }

        @Test
        void 모든_시간이_가득_찬_날짜는_예약_불가하다() {
            // given
            String date = "2025-07";
            LocalDate targetDate = LocalDate.of(2025, 7, 1);

            givenNextMonthReservation(date);
            givenAllReservedTimeSlots(targetDate);

            // when
            AvailableDateResponse response =
                    memberReservationService.findAvailableDate(popupId, date);

            // then
            assertPopupDate(response);

            ReservableDate reservableDate =
                    response.reservableDate().stream()
                            .filter(d -> d.date().equals(targetDate))
                            .findFirst()
                            .orElseThrow();

            assertThat(reservableDate.isReservable()).isFalse();
            reservableDate.timeSlots().forEach(slot -> assertThat(slot.isPossible()).isFalse());

            verify(memberReservationRepository)
                    .findDailyReservationCount(
                            eq(popupId),
                            eq(LocalDate.of(2025, 6, 30)),
                            eq(LocalDate.of(2025, 7, 2)),
                            eq("2025-07"));
        }

        @Test
        void 예약_자리가_남으면_TRUE_차있으면_FALSE가_표시된다() {
            // given
            String date = "2025-07";
            LocalDate targetDate = LocalDate.of(2025, 7, 2);

            givenNextMonthReservation(date);
            givenPartiallyReservedTimeSlots(targetDate);

            // when
            AvailableDateResponse response =
                    memberReservationService.findAvailableDate(popupId, date);

            // then
            assertPopupDate(response);

            ReservableDate reservableDate =
                    response.reservableDate().stream()
                            .filter(d -> d.date().equals(targetDate))
                            .findFirst()
                            .orElseThrow();

            assertThat(reservableDate.isReservable()).isTrue();

            for (ReservableTime timeSlot : reservableDate.timeSlots()) {
                if (timeSlot.time().equals(LocalTime.of(13, 0))) {
                    assertThat(timeSlot.isPossible()).isFalse();
                } else {
                    assertThat(timeSlot.isPossible()).isTrue();
                }
            }

            verify(memberReservationRepository)
                    .findDailyReservationCount(
                            eq(popupId),
                            eq(LocalDate.of(2025, 6, 30)),
                            eq(LocalDate.of(2025, 7, 2)),
                            eq("2025-07"));
        }

        @Test
        void 예약_기간_아닌경우_빈_리스트_반환한다() {
            // given
            String date = "2025-08";

            given(managerServiceClient.findMonthlyReservation(eq(popupId), eq(date)))
                    .willReturn(
                            new MonthlyReservationResponse(
                                    LocalDate.of(2025, 6, 30),
                                    LocalDate.of(2025, 7, 2),
                                    5,
                                    Collections.emptyList()));

            // when
            AvailableDateResponse response =
                    memberReservationService.findAvailableDate(popupId, date);

            // then
            assertThat(response.reservableDate()).isEmpty();
        }

        @Test
        void 존재하지_않는_팝업ID는_예외처리_된다() throws Exception {
            // given
            Long popupId = 999L;
            String date = "2025-07";

            given(managerServiceClient.findMonthlyReservation(popupId, date))
                    .willThrow(buildMonthlyReservationException(popupId, date));

            // when
            Throwable thrown =
                    catchThrowable(() -> memberReservationService.findAvailableDate(popupId, date));

            // then
            assertThat(thrown).isInstanceOf(FeignException.class);

            String body = ((FeignException) thrown).contentUTF8();
            String actualMessage =
                    new ObjectMapper().readTree(body).path("data").path("message").asText();

            assertThat(actualMessage).isEqualTo("해당 팝업이 존재하지 않습니다.");
        }
    }

    private void givenThisMonthReservation(String date) {
        given(managerServiceClient.findMonthlyReservation(eq(popupId), eq(date)))
                .willReturn(
                        new MonthlyReservationResponse(
                                LocalDate.of(2025, 6, 30),
                                LocalDate.of(2025, 7, 2),
                                3,
                                List.of(
                                        new DailyReservation(
                                                LocalDate.of(2025, 6, 30),
                                                List.of(
                                                        new TimeSlot(1L, LocalTime.of(12, 0)),
                                                        new TimeSlot(2L, LocalTime.of(13, 0)),
                                                        new TimeSlot(3L, LocalTime.of(14, 0)))))));
    }

    private void givenNextMonthReservation(String date) {
        given(managerServiceClient.findMonthlyReservation(eq(popupId), eq(date)))
                .willReturn(
                        new MonthlyReservationResponse(
                                LocalDate.of(2025, 6, 30),
                                LocalDate.of(2025, 7, 2),
                                3,
                                List.of(
                                        new DailyReservation(
                                                LocalDate.of(2025, 7, 1),
                                                List.of(
                                                        new TimeSlot(4L, LocalTime.of(12, 0)),
                                                        new TimeSlot(5L, LocalTime.of(13, 0)),
                                                        new TimeSlot(6L, LocalTime.of(14, 0)))),
                                        new DailyReservation(
                                                LocalDate.of(2025, 7, 2),
                                                List.of(
                                                        new TimeSlot(7L, LocalTime.of(12, 0)),
                                                        new TimeSlot(8L, LocalTime.of(13, 0)),
                                                        new TimeSlot(9L, LocalTime.of(14, 0)))))));
    }

    private void givenAllReservedTimeSlots(LocalDate targetDate) {
        List<HourlyReservationCount> hourlyCounts =
                List.of(
                        new HourlyReservationCount(LocalTime.of(12, 0), 3),
                        new HourlyReservationCount(LocalTime.of(13, 0), 3),
                        new HourlyReservationCount(LocalTime.of(14, 0), 3));

        List<DailyReservationCountResponse> countResponseList =
                List.of(new DailyReservationCountResponse(targetDate, hourlyCounts));

        given(
                        memberReservationRepository.findDailyReservationCount(
                                eq(popupId),
                                eq(LocalDate.of(2025, 6, 30)),
                                eq(LocalDate.of(2025, 7, 2)),
                                eq("2025-07")))
                .willReturn(countResponseList);
    }

    private void givenPartiallyReservedTimeSlots(LocalDate targetDate) {
        List<HourlyReservationCount> hourlyCounts =
                List.of(
                        new HourlyReservationCount(LocalTime.of(12, 0), 0),
                        new HourlyReservationCount(LocalTime.of(13, 0), 3),
                        new HourlyReservationCount(LocalTime.of(14, 0), 0));

        List<DailyReservationCountResponse> countResponseList =
                List.of(new DailyReservationCountResponse(targetDate, hourlyCounts));

        given(
                        memberReservationRepository.findDailyReservationCount(
                                eq(popupId),
                                eq(LocalDate.of(2025, 6, 30)),
                                eq(LocalDate.of(2025, 7, 2)),
                                eq("2025-07")))
                .willReturn(countResponseList);
    }

    private void givenEmptyReservedTimeSlots(LocalDate targetDate) {
        List<HourlyReservationCount> hourlyCounts =
                List.of(
                        new HourlyReservationCount(LocalTime.of(12, 0), 0),
                        new HourlyReservationCount(LocalTime.of(13, 0), 0),
                        new HourlyReservationCount(LocalTime.of(14, 0), 0));

        List<DailyReservationCountResponse> countResponseList =
                List.of(new DailyReservationCountResponse(targetDate, hourlyCounts));

        given(
                        memberReservationRepository.findDailyReservationCount(
                                eq(popupId),
                                eq(LocalDate.of(2025, 6, 30)),
                                eq(LocalDate.of(2025, 7, 2)),
                                eq("2025-06")))
                .willReturn(countResponseList);
    }

    private FeignException buildMonthlyReservationException(Long popupId, String date) {
        String errorResponse =
                """
                {
                  "success": false,
                  "status": 404,
                  "data": {
                    "errorClassName": "POPUP_NOT_FOUND",
                    "message": "해당 팝업이 존재하지 않습니다."
                  }
                }
                """;
        Request request =
                Request.create(
                        Request.HttpMethod.GET,
                        "/internal/reservations/popups/" + popupId + "?date=" + date,
                        Map.of("Content-Type", List.of("application/json")),
                        null,
                        new RequestTemplate());

        return FeignException.errorStatus(
                "findMonthlyReservation",
                Response.builder()
                        .status(404)
                        .reason("Not Found")
                        .request(request)
                        .body(errorResponse, UTF_8)
                        .build());
    }

    @Nested
    class 설문지_조회할_때 {

        @Test
        void 팝업이_존재하면_정상적으로_조회된다() {
            // given
            givenSurveyChoices();

            // when
            List<SurveyChoiceResponse> choices =
                    memberReservationService.findSurveyChoicesByPopupId(popupId);

            // then
            assertThat(choices).hasSize(4);
            assertSurveyChoice(choices.get(0), 1L, 1L);
            assertSurveyChoice(choices.get(1), 2L, 6L);
            assertSurveyChoice(choices.get(2), 3L, 11L);
            assertSurveyChoice(choices.get(3), 4L, 16L);
        }
    }

    private void givenSurveyChoices() {
        List<SurveyChoiceResponse> surveyChoices =
                List.of(
                        new SurveyChoiceResponse(
                                1L,
                                List.of(
                                        new SurveyOption(1L, "보기1"),
                                        new SurveyOption(2L, "보기2"),
                                        new SurveyOption(3L, "보기3"),
                                        new SurveyOption(4L, "보기4"),
                                        new SurveyOption(5L, "보기5"))),
                        new SurveyChoiceResponse(
                                2L,
                                List.of(
                                        new SurveyOption(6L, "보기1"),
                                        new SurveyOption(7L, "보기2"),
                                        new SurveyOption(8L, "보기3"),
                                        new SurveyOption(9L, "보기4"),
                                        new SurveyOption(10L, "보기5"))),
                        new SurveyChoiceResponse(
                                3L,
                                List.of(
                                        new SurveyOption(11L, "보기1"),
                                        new SurveyOption(12L, "보기2"),
                                        new SurveyOption(13L, "보기3"),
                                        new SurveyOption(14L, "보기4"),
                                        new SurveyOption(15L, "보기5"))),
                        new SurveyChoiceResponse(
                                4L,
                                List.of(
                                        new SurveyOption(16L, "보기1"),
                                        new SurveyOption(17L, "보기2"),
                                        new SurveyOption(18L, "보기3"),
                                        new SurveyOption(19L, "보기4"),
                                        new SurveyOption(20L, "보기5"))));

        given(managerServiceClient.findSurveyChoicesByPopupId(anyLong())).willReturn(surveyChoices);
    }

    private void assertSurveyChoice(
            SurveyChoiceResponse response, Long expectedSurveyId, Long startingChoiceId) {

        assertThat(response.surveyId()).isEqualTo(expectedSurveyId);

        List<SurveyOption> options = response.options();
        assertThat(options).hasSize(5);

        for (int i = 0; i < options.size(); i++) {
            SurveyOption option = options.get(i);
            Long choiceIdExpected = startingChoiceId + i;
            String contentExpected = "보기" + (i + 1);

            assertThat(option.choiceId()).isEqualTo(choiceIdExpected);

            assertThat(option.content()).isNotBlank().isEqualTo(contentExpected);
        }
    }

    @Nested
    class 설문지_등록_할_때 {
        @Test
        void 설문지_응답이_4개가_아니면_예외가_발생한다() {
            // given
            List<SurveyChoiceRequest> invalidChoices =
                    List.of(
                            new SurveyChoiceRequest(1L, 1L),
                            new SurveyChoiceRequest(2L, 2L),
                            new SurveyChoiceRequest(3L, 3L));

            // when & then
            assertThatThrownBy(
                            () ->
                                    memberReservationService.createMemberAnswer(
                                            popupId, memberId, invalidChoices))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode", MemberReservationErrorCode.INVALID_SURVEY_CHOICES_COUNT);
        }

        @Test
        void 설문지_응답이_4개면_producer_로_전송한다() {
            // given
            List<SurveyChoiceRequest> validChoices =
                    List.of(
                            new SurveyChoiceRequest(1L, 1L),
                            new SurveyChoiceRequest(2L, 6L),
                            new SurveyChoiceRequest(3L, 11L),
                            new SurveyChoiceRequest(4L, 16L));

            // when
            memberReservationService.createMemberAnswer(popupId, memberId, validChoices);

            // then
            verify(memberAnswerProducer, times(1)).sendMessage(any());
        }
    }

    @Nested
    class 회원이_예약을_시도할_때 {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(
                    memberReservationService, "reservationRedisTemplate", reservationRedisTemplate);
        }

        @Test
        void 자리가_존재하고_예약_가능한_상태이면_예약에_성공한다() {
            // given
            stubExistMemberReservationAndRedis(false, () -> 1L, null, true);
            ArgumentCaptor<MemberReservation> captor =
                    ArgumentCaptor.forClass(MemberReservation.class);

            // when
            memberReservationService.createMemberReservation(memberId, reservationId);

            // then
            verify(memberReservationRepository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getMemberId()).isEqualTo(Long.parseLong(memberId));
            verify(reservationRedisValueOperations, times(1)).decrement(reservationId.toString());
            verify(eventPublisher, times(1)).publishEvent(any(MemberReservationUpdateEvent.class));
        }

        @Test
        void 이미_예약한_사용자는_예외가_발생한다() {
            // given
            stubExistMemberReservationAndRedis(true, null, null, null);

            // when & then
            assertThatThrownBy(
                            () ->
                                    memberReservationService.createMemberReservation(
                                            memberId, reservationId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(
                            MemberReservationErrorCode.RESERVATION_ALREADY_EXISTS.getMessage());
        }

        @Test
        void redis_차감이_실패하면_예외가_발생한다() {
            // given
            stubExistMemberReservationAndRedis(
                    false,
                    () -> {
                        throw new RedisConnectionFailureException("Redis down");
                    },
                    null,
                    null);

            // when & then
            assertThatThrownBy(
                            () ->
                                    memberReservationService.createMemberReservation(
                                            memberId, reservationId))
                    .isInstanceOf(RedisConnectionFailureException.class)
                    .hasMessageContaining("Redis down");

            verify(reservationRedisValueOperations, times(1)).decrement(reservationId.toString());
        }

        @Test
        void 회원_예약_저장에_실패하면_Redis_복구가_일어난다() {
            // given
            stubExistMemberReservationAndRedis(false, () -> 0L, () -> 1L, false);

            // when & then
            assertThatThrownBy(
                            () ->
                                    memberReservationService.createMemberReservation(
                                            memberId, reservationId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(RESERVATION_FAILED.getMessage());

            verify(reservationRedisValueOperations, times(1)).decrement(reservationId.toString());
            verify(reservationRedisValueOperations, times(1)).increment(reservationId.toString());
        }

        @Test
        void 자리가_없으면_예약에_실패하고_Redis_복구가_일어난다() {
            // given
            stubExistMemberReservationAndRedis(false, () -> -1L, () -> 0L, null);

            // when & then
            assertThatThrownBy(
                            () ->
                                    memberReservationService.createMemberReservation(
                                            memberId, reservationId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(RESERVATION_FAILED.getMessage());

            verify(reservationRedisValueOperations, times(1)).decrement(reservationId.toString());
            verify(reservationRedisValueOperations, times(1)).increment(reservationId.toString());
        }

        private void stubExistMemberReservationAndRedis(
                Boolean exist,
                Supplier<Long> decrementStub,
                Supplier<Long> incrementStub,
                Boolean save) {
            given(
                            memberReservationRepository
                                    .existsMemberReservationByMemberIdAndReservationId(
                                            anyLong(), anyLong()))
                    .willReturn(exist);

            if (!exist)
                given(reservationRedisTemplate.opsForValue())
                        .willReturn(reservationRedisValueOperations);

            if (decrementStub != null)
                given(reservationRedisValueOperations.decrement(anyString()))
                        .willAnswer(inv -> decrementStub.get());

            if (incrementStub != null)
                given(reservationRedisValueOperations.increment(anyString()))
                        .willAnswer(inv -> incrementStub.get());

            if (save != null) {
                if (save) {
                    given(memberReservationRepository.save(any(MemberReservation.class)))
                            .willReturn(
                                    MemberReservation.createMemberReservation(
                                            reservationId, Long.parseLong(memberId)));
                } else {
                    doThrow(new RuntimeException("DB error"))
                            .when(memberReservationRepository)
                            .save(any(MemberReservation.class));
                }
            }
        }
    }

    @Nested
    class 회원예약_생성_후_업데이트_할_때 {
        @Test
        void 회원예약_업데이트에_성공한다() {
            // given
            MemberReservation reservation = mock(MemberReservation.class);
            given(memberReservationRepository.findById(anyLong()))
                    .willReturn(Optional.of(reservation));
            given(memberServiceClient.findMemberInfo(anyLong()))
                    .willReturn(
                            new MemberInternalInfoResponse(
                                    Long.parseLong(memberId),
                                    "testNickName",
                                    MemberAge.TEENAGER,
                                    MemberGender.MALE,
                                    MemberRole.USER,
                                    MemberStatus.NORMAL));

            given(managerServiceClient.findReservationById(anyLong()))
                    .willReturn(
                            new ReservationInfoResponse(
                                    popupId, LocalDate.now().plusDays(30), LocalTime.of(17, 0)));

            // when
            memberReservationService.updateMemberReservation(memberReservationId);

            // then
            verify(memberReservationRepository, times(1)).findById(anyLong());
            verify(memberServiceClient, times(1)).findMemberInfo(anyLong());
            verify(eventPublisher, times(1))
                    .publishEvent(any(MemberReservationNotificationEvent.class));
        }

        @Test
        void 존재하지_않는_회원예약이면_예외가_발생한다() {
            // given
            given(memberReservationRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberReservationService.updateMemberReservation(999L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(
                            MemberReservationErrorCode.MEMBER_RESERVATION_NOT_FOUND.getMessage());
        }

        @Test
        void 회원_정보가_존재하지_않으면_예외가_발생한다() throws JsonProcessingException {
            // given
            MemberReservation reservation = mock(MemberReservation.class);
            ;
            given(memberReservationRepository.findById(anyLong()))
                    .willReturn(Optional.of(reservation));
            given(memberServiceClient.findMemberInfo(anyLong()))
                    .willThrow(buildMemberInfoException(Long.parseLong(memberId)));

            // when
            Throwable thrown =
                    catchThrowable(
                            () ->
                                    memberReservationService.updateMemberReservation(
                                            memberReservationId));

            // then
            assertThat(thrown).isInstanceOf(FeignException.class);
            String body = ((FeignException) thrown).contentUTF8();
            String actualMessage =
                    new ObjectMapper().readTree(body).path("data").path("message").asText();
            assertThat(actualMessage).isEqualTo("회원을 찾을 수 없습니다.");
        }

        @Test
        void 예약_정보가_존재하지_않으면_예외가_발생한다() throws JsonProcessingException {
            // given
            MemberReservation reservation = mock(MemberReservation.class);
            ;
            given(memberReservationRepository.findById(anyLong()))
                    .willReturn(Optional.of(reservation));
            given(managerServiceClient.findReservationById(anyLong()))
                    .willThrow(buildReservationInfoException(popupId));

            // when
            Throwable thrown =
                    catchThrowable(
                            () ->
                                    memberReservationService.updateMemberReservation(
                                            memberReservationId));

            // then
            assertThat(thrown).isInstanceOf(FeignException.class);
            String body = ((FeignException) thrown).contentUTF8();
            String actualMessage =
                    new ObjectMapper().readTree(body).path("data").path("message").asText();
            assertThat(actualMessage).isEqualTo("해당 예약을 찾을 수 없습니다.");
        }
    }

    @Nested
    class 회원예약_취소할_때 {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(
                    memberReservationService, "reservationRedisTemplate", reservationRedisTemplate);
            ReflectionTestUtils.setField(
                    memberReservationService,
                    "notificationRedisTemplate",
                    notificationRedisTemplate);
        }

        @Test
        void 회원예약_취소에_성공한다() {
            // given
            ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
            MemberReservation reservation =
                    MemberReservation.createMemberReservation(
                            reservationId, Long.parseLong(memberId));
            given(memberReservationRepository.findById(memberReservationId))
                    .willReturn(Optional.of(reservation));
            given(reservationRedisTemplate.opsForValue())
                    .willReturn(reservationRedisValueOperations);
            given(reservationRedisValueOperations.increment(anyString())).willAnswer(inv -> 1L);
            given(notificationRedisTemplate.opsForZSet()).willReturn(zSetOperations);

            // when
            memberReservationService.cancelMemberReservation(memberReservationId);

            // then
            verify(memberReservationRepository, times(1)).delete(reservation);
            verify(reservationRedisValueOperations, times(1)).increment(reservationId.toString());
            verify(notificationRedisTemplate.opsForZSet(), times(1))
                    .remove("reservation:notifications", reservationId + "|" + memberId);
        }

        @Test
        void 존재하지_않는_회원예약이면_예외가_발생한다() {
            // given
            given(memberReservationRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberReservationService.cancelMemberReservation(999L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(
                            MemberReservationErrorCode.MEMBER_RESERVATION_NOT_FOUND.getMessage());
        }

        @Test
        void 예약ID가_null이면_예외가_발생한다() {
            // given
            MemberReservation reservation = mock(MemberReservation.class);
            given(reservation.getReservationId()).willReturn(null);
            given(memberReservationRepository.findById(anyLong()))
                    .willReturn(Optional.of(reservation));

            // when & then
            assertThatThrownBy(() -> memberReservationService.cancelMemberReservation(1L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(MemberReservationErrorCode.RESERVATION_NOT_FOUND.getMessage());
        }
    }

    @Nested
    class 예약_목록_조회할_때 {

        @Test
        void 완료된_예약이_존재하면_조회에_성공한다() {
            // given
            MemberReservation reservation =
                    MemberReservation.createMemberReservation(1L, Long.parseLong(memberId));
            reservation.updateMemberReservation(
                    popupId, "qrImage", LocalDate.of(2025, 7, 1), LocalTime.of(12, 0));

            given(memberReservationRepository.findByMemberIdAndStatus(anyLong(), eq(RESERVED)))
                    .willReturn(List.of(reservation));

            given(managerServiceClient.findReservedPopupInfoList(any()))
                    .willReturn(
                            List.of(
                                    new ReservationPopupInfoResponse(
                                            popupId,
                                            "BLACK PINK 팝업스토어",
                                            "서울특별시 영등포구 여의대로 108, 5층",
                                            37.527097,
                                            126.927301)));

            // when
            List<ReservationDetailResponse> response =
                    memberReservationService.findReservationInfo(memberId);

            // then
            assertThat(response).hasSize(1);
            ReservationDetailResponse reservationDetail = response.get(0);

            assertAll(
                    () -> assertThat(reservationDetail.popupId()).isEqualTo(popupId),
                    () -> assertThat(reservationDetail.popupName()).isEqualTo("BLACK PINK 팝업스토어"),
                    () -> assertThat(reservationDetail.reservationDate()).isEqualTo("2025-07-01"),
                    () -> assertThat(reservationDetail.reservationTime()).isEqualTo("12:00"),
                    () -> assertThat(reservationDetail.reservationDay()).isEqualTo("TUE"),
                    () ->
                            assertThat(reservationDetail.address())
                                    .isEqualTo("서울특별시 영등포구 여의대로 108, 5층"),
                    () -> assertThat(reservationDetail.latitude()).isEqualTo(37.527097),
                    () -> assertThat(reservationDetail.longitude()).isEqualTo(126.927301),
                    () -> assertThat(reservationDetail.qrImage()).isEqualTo("qrImage"));

            verify(memberReservationRepository).findByMemberIdAndStatus(anyLong(), eq(RESERVED));
            verify(managerServiceClient).findReservedPopupInfoList(any());
        }

        @Test
        void 대기중인_예약만_존재하면_빈_리스트를_반환한다() {
            // given
            given(memberReservationRepository.findByMemberIdAndStatus(anyLong(), eq(RESERVED)))
                    .willReturn(Collections.emptyList());

            // when
            List<ReservationDetailResponse> result =
                    memberReservationService.findReservationInfo(memberId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void 예약_내역이_존재하지_않으면_빈_리스트를_반환한다() {
            // given
            given(memberReservationRepository.findByMemberIdAndStatus(anyLong(), eq(RESERVED)))
                    .willReturn(Collections.emptyList());

            // when
            List<ReservationDetailResponse> result =
                    memberReservationService.findReservationInfo(memberId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void 예약_날짜_및_시간이_오늘보다_이전인_경우_빈_리스트를_반환한다() {
            // given
            given(memberReservationRepository.findByMemberIdAndStatus(anyLong(), eq(RESERVED)))
                    .willReturn(Collections.emptyList());

            // when
            List<ReservationDetailResponse> result =
                    memberReservationService.findReservationInfo(memberId);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class 인기_팝업_아이디를_조회할_때 {

        @Test
        void 예약된_팝업이_4개_이상이면_크기가_4인_리스트를_예약자_수_내림차순으로_반환한다() {
            // given
            given(memberReservationRepository.findHotPopupIds())
                    .willReturn(List.of(1L, 2L, 3L, 4L));

            // when
            List<Long> result = memberReservationService.findHotPopupIds();

            // then
            assertThat(result).containsExactly(1L, 2L, 3L, 4L);
            verify(memberReservationRepository, times(1)).findHotPopupIds();
        }

        @Test
        void 예약자_수가_같으면_팝업_아이디_오름차순으로_반환한다() {
            // given
            given(memberReservationRepository.findHotPopupIds())
                    .willReturn(List.of(1L, 2L, 3L, 4L));

            // when
            List<Long> result = memberReservationService.findHotPopupIds();

            // then
            assertThat(result).containsExactly(1L, 2L, 3L, 4L);
        }

        @Test
        void 예약된_팝업이_2개인_경우_크기가_2인_리스트를_예약자_수_내림차순으로_반환한다() {
            // given
            given(memberReservationRepository.findHotPopupIds()).willReturn(List.of(2L, 1L));

            // when
            List<Long> result = memberReservationService.findHotPopupIds();

            // then
            assertThat(result).containsExactly(2L, 1L);

            verify(memberReservationRepository).findHotPopupIds();
        }

        @Test
        void 예약된_팝업이_없는_경우_빈_리스트를_반환한다() {
            // given
            given(memberReservationRepository.findHotPopupIds())
                    .willReturn(Collections.emptyList());

            // when
            List<Long> result = memberReservationService.findHotPopupIds();

            // then
            assertThat(result).isEmpty();

            verify(memberReservationRepository).findHotPopupIds();
        }
    }

    @Nested
    class 가장_가까운_예약_조회할_때 {
        @Test
        void 현재_시간_이후의_완료된_예약이_존재하면_가장_가까운_예약_조회에_성공한다() {
            // given
            LocalDate now = LocalDate.now();

            MemberReservation reservation =
                    MemberReservation.createMemberReservation(1L, Long.parseLong(memberId));
            reservation.updateMemberReservation(
                    popupId, "qrImage", now.plusDays(1), LocalTime.of(12, 0));

            given(memberReservationRepository.findUpcomingReservation(any()))
                    .willReturn(reservation);

            given(managerServiceClient.findReservedPopupInfo(any()))
                    .willReturn(
                            new ReservationPopupInfoResponse(
                                    popupId,
                                    "BLACK PINK 팝업스토어",
                                    "서울특별시 강남구 테헤란로 12, 1층 201호",
                                    37.527097,
                                    126.927301));

            // when
            ReservationDetailResponse response =
                    memberReservationService.findUpcomingReservationInfo(memberId);

            // then
            assertAll(
                    () -> assertThat(response.popupId()).isEqualTo(popupId),
                    () -> assertThat(response.popupName()).isEqualTo("BLACK PINK 팝업스토어"),
                    () ->
                            assertThat(response.reservationDate())
                                    .isEqualTo(now.plusDays(1).toString()),
                    () -> assertThat(response.reservationTime()).isEqualTo("12:00"),
                    () ->
                            assertThat(response.reservationDay())
                                    .isEqualTo(
                                            now.plusDays(1)
                                                    .getDayOfWeek()
                                                    .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                                                    .toUpperCase()),
                    () -> assertThat(response.address()).isEqualTo("서울특별시 강남구 테헤란로 12, 1층 201호"),
                    () -> assertThat(response.latitude()).isEqualTo(37.527097),
                    () -> assertThat(response.longitude()).isEqualTo(126.927301),
                    () -> assertThat(response.qrImage()).isEqualTo("qrImage"));

            verify(memberReservationRepository).findUpcomingReservation(any());
            verify(managerServiceClient).findReservedPopupInfo(any());
        }

        @Test
        void 예약했던_목록들이_현재_시간보다_이전이면_NULL을_반환한다() {
            // given
            given(memberReservationRepository.findUpcomingReservation(anyLong())).willReturn(null);

            // when & then
            assertUpcomingReservationIsNull();

            verify(memberReservationRepository).findUpcomingReservation(anyLong());
        }

        @Test
        void 예약_정보가_존재하지_않으면_NULL을_반환한다() {
            // given
            given(memberReservationRepository.findUpcomingReservation(any())).willReturn(null);

            // when & then
            assertUpcomingReservationIsNull();
        }

        @Test
        void 현재_시간_이후_대기중인_예약이_존재하면_NULL을_반환한다() {
            // given
            given(memberReservationRepository.findUpcomingReservation(anyLong())).willReturn(null);

            // when & then
            assertUpcomingReservationIsNull();
        }
    }

    private void assertPopupDate(AvailableDateResponse response) {
        assertAll(
                () -> assertThat(response.popupOpenDate()).isEqualTo("2025-06-30"),
                () -> assertThat(response.popupCloseDate()).isEqualTo("2025-07-02"));
    }

    private void assertUpcomingReservationIsNull() {
        ReservationDetailResponse response =
                memberReservationService.findUpcomingReservationInfo(memberId);
        assertThat(response).isNull();
    }

    private FeignException buildMemberInfoException(Long memberId) {
        String errorResponse =
                """
                {
                  "success": false,
                  "status": 404,
                  "data": {
                    "errorClassName": "MEMBER_NOT_FOUND",
                    "message": "회원을 찾을 수 없습니다."
                  }
                }
                """;
        Request request =
                Request.create(
                        Request.HttpMethod.GET,
                        "/internal/members/" + memberId,
                        Map.of("Content-Type", List.of("application/json")),
                        null,
                        new RequestTemplate());

        return FeignException.errorStatus(
                "findMemberInfo",
                Response.builder()
                        .status(404)
                        .reason("Not Found")
                        .request(request)
                        .body(errorResponse, UTF_8)
                        .build());
    }

    private FeignException buildReservationInfoException(Long reservationId) {
        String errorResponse =
                """
                {
                  "success": false,
                  "status": 404,
                  "data": {
                    "errorClassName": "RESERVATION_NOT_FOUND",
                    "message": "해당 예약을 찾을 수 없습니다."
                  }
                }
                """;
        Request request =
                Request.create(
                        Request.HttpMethod.GET,
                        "/internal/reservations/" + reservationId,
                        Map.of("Content-Type", List.of("application/json")),
                        null,
                        new RequestTemplate());

        return FeignException.errorStatus(
                "findReservationById",
                Response.builder()
                        .status(404)
                        .reason("Not Found")
                        .request(request)
                        .body(errorResponse, UTF_8)
                        .build());
    }

    @Nested
    class 오늘_예약자_수_조회할_때 {

        @Test
        void 예약자_수가_존재하면_성공한다() {
            // given
            DailyMemberReservationCountResponse dailyMemberReservationCountResponse =
                    mock(DailyMemberReservationCountResponse.class);
            given(
                            memberReservationRepository.findDailyMemberReservationCount(
                                    anyLong(), any(LocalDate.class)))
                    .willReturn(dailyMemberReservationCountResponse);

            // when
            memberReservationService.findDailyMemberReservationCount(popupId);

            // then
            verify(memberReservationRepository, times(1))
                    .findDailyMemberReservationCount(eq(popupId), any(LocalDate.class));
        }
    }

    @Nested
    class 회원이_팝업_입장할_때 {

        @Test
        void 현재시간이_입장시간_30분_이내이고_입장한_적이_없으면_성공한다() {
            // given
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();
            MemberReservation memberReservation =
                    createMemberReservation(
                            false, popupId, memberReservationId, today, now.minusMinutes(5));
            given(memberReservationRepository.findById(memberReservationId))
                    .willReturn(Optional.of(memberReservation));

            QrEntranceInfoRequest request = createQrRequest(today, now.minusMinutes(5));

            // when
            memberReservationService.isEnterancePossible(request, popupId);

            // then
            verify(memberReservation).updateIsEntered();
            verify(eventPublisher).publishEvent(any(MemberEnteredMessage.class));
        }

        @Test
        void 존재하지_않는_회원예약이면_예외가_발생한다() {
            // given
            given(memberReservationRepository.findById(memberReservationId))
                    .willReturn(Optional.empty());

            QrEntranceInfoRequest request = createQrRequest(LocalDate.now(), LocalTime.now());

            // when & then
            assertThatThrownBy(() -> memberReservationService.isEnterancePossible(request, popupId))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(
                            MemberReservationErrorCode.MEMBER_RESERVATION_NOT_FOUND.getMessage());
        }

        @Test
        void 이미_입장한_회원예약이면_예외가_발생한다() {
            // given
            MemberReservation memberReservation =
                    createMemberReservation(true, null, null, null, null);
            given(memberReservationRepository.findById(memberReservationId))
                    .willReturn(Optional.of(memberReservation));

            QrEntranceInfoRequest request =
                    createQrRequest(LocalDate.now(), LocalTime.now().minusMinutes(5));

            // when & then
            assertThatThrownBy(() -> memberReservationService.isEnterancePossible(request, popupId))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(
                            MemberReservationErrorCode.RESERVATION_ALREADY_ENTERED.getMessage());
        }

        @Test
        void 팝업ID가_일치하지_않으면_예외가_발생한다() {
            // given
            MemberReservation memberReservation =
                    createMemberReservation(false, 999L, null, null, null);
            given(memberReservationRepository.findById(memberReservationId))
                    .willReturn(Optional.of(memberReservation));

            QrEntranceInfoRequest request =
                    createQrRequest(LocalDate.now(), LocalTime.now().minusMinutes(5));

            // when & then
            assertThatThrownBy(() -> memberReservationService.isEnterancePossible(request, popupId))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(MemberReservationErrorCode.RESERVATION_POPUP_MISMATCH.getMessage());
        }

        @Test
        void QR에_저장된_예약_ID와__실제_예약_ID가_일치하지_않으면_예외가_발생한다() {
            // given
            MemberReservation memberReservation =
                    createMemberReservation(false, popupId, 888L, null, null);
            given(memberReservationRepository.findById(memberReservationId))
                    .willReturn(Optional.of(memberReservation));

            QrEntranceInfoRequest request =
                    createQrRequest(LocalDate.now(), LocalTime.now().minusMinutes(5));

            // when & then
            assertThatThrownBy(() -> memberReservationService.isEnterancePossible(request, popupId))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(MemberReservationErrorCode.INVALID_QR_CODE.getMessage());
        }

        @Test
        void 예약날짜가_오늘이_아니면_예외가_발생한다() {
            // given
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalTime now = LocalTime.now();
            MemberReservation memberReservation =
                    createMemberReservation(false, popupId, reservationId, yesterday, null);
            given(memberReservationRepository.findById(memberReservationId))
                    .willReturn(Optional.of(memberReservation));

            QrEntranceInfoRequest request = createQrRequest(yesterday, now.minusMinutes(5));

            // when & then
            assertThatThrownBy(() -> memberReservationService.isEnterancePossible(request, popupId))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(MemberReservationErrorCode.RESERVATION_DATE_MISMATCH.getMessage());
        }

        @Test
        void 현재시간이_입장시간_전이면_예외가_발생한다() {
            // given
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();
            MemberReservation reservation =
                    createMemberReservation(
                            false, popupId, reservationId, today, now.plusMinutes(10));
            when(memberReservationRepository.findById(memberReservationId))
                    .thenReturn(Optional.of(reservation));

            QrEntranceInfoRequest request = createQrRequest(today, now.plusMinutes(5));

            // when & then
            assertThatThrownBy(() -> memberReservationService.isEnterancePossible(request, popupId))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(MemberReservationErrorCode.RESERVATION_TIME_MISMATCH.getMessage());
        }

        @Test
        void 현재시간이_입장시간_30분_이내가_아니면_예외가_발생한다() {
            // given
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();
            MemberReservation reservation =
                    createMemberReservation(
                            false, popupId, reservationId, today, now.minusMinutes(40));
            when(memberReservationRepository.findById(memberReservationId))
                    .thenReturn(Optional.of(reservation));

            QrEntranceInfoRequest request = createQrRequest(today, now.minusMinutes(31));

            // when & then
            assertThatThrownBy(() -> memberReservationService.isEnterancePossible(request, popupId))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(MemberReservationErrorCode.RESERVATION_TIME_MISMATCH.getMessage());
        }

        private MemberReservation createMemberReservation(
                Boolean isEntered,
                Long popupId,
                Long reservationId,
                LocalDate date,
                LocalTime time) {
            MemberReservation memberReservation = mock(MemberReservation.class);
            if (isEntered != null) given(memberReservation.getIsEntered()).willReturn(isEntered);
            if (popupId != null) given(memberReservation.getPopupId()).willReturn(popupId);
            if (reservationId != null)
                given(memberReservation.getReservationId()).willReturn(reservationId);
            if (date != null) given(memberReservation.getReservationDate()).willReturn(date);
            if (time != null) given(memberReservation.getReservationTime()).willReturn(time);
            return memberReservation;
        }

        private QrEntranceInfoRequest createQrRequest(LocalDate date, LocalTime time) {
            return new QrEntranceInfoRequest(
                    memberReservationId,
                    reservationId,
                    popupId,
                    MemberAge.TWENTIES,
                    MemberGender.MALE,
                    date,
                    time);
        }
    }
}
