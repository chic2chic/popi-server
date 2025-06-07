package com.lgcns.service.unit;

import static com.lgcns.domain.MemberReservationStatus.RESERVED;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.client.managerClient.ManagerServiceClient;
import com.lgcns.client.managerClient.dto.response.DailyReservation;
import com.lgcns.client.managerClient.dto.response.MonthlyReservationResponse;
import com.lgcns.client.managerClient.dto.response.ReservationPopupInfoResponse;
import com.lgcns.client.managerClient.dto.response.TimeSlot;
import com.lgcns.client.memberClient.MemberServiceClient;
import com.lgcns.domain.MemberReservation;
import com.lgcns.dto.request.SurveyChoiceRequest;
import com.lgcns.dto.response.*;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.MemberReservationErrorCode;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
public class MemberReservationServiceUnitTest {

    @InjectMocks MemberReservationServiceImpl memberReservationService;

    @Mock MemberReservationRepository memberReservationRepository;

    @Mock ManagerServiceClient managerServiceClient;

    @Mock MemberServiceClient memberServiceClient;

    @Mock private RedisTemplate<String, Long> reservationRedisTemplate;

    @Mock private RedisTemplate<String, String> notificationRedisTemplate;

    @Mock MemberAnswerProducer memberAnswerProducer;

    private final String memberId = "1";
    private final Long popupId = 1L;

    @Nested
    class 예약_가능한_날짜를_조회할_때 {

        @Test
        void 날짜에_문자가_포함되면_예외가_발생한다() {
            // given
            String invalidDate = "2025-July";

            // when & then)
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

    // TODO 예약 생성할 때

    // TODO 예약 업데이트할 때

    // TODO 예약 취소할 때

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

    // TODO 오늘 예약자 수 조회할 때

    // TODO 회원이 팝업 입장할 때

}
