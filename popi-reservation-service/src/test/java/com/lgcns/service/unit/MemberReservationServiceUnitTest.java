package com.lgcns.service.unit;

import static com.lgcns.domain.MemberReservationStatus.RESERVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
            String date = "2025-07";

            MonthlyReservationResponse monthlyReservationResponse =
                    new MonthlyReservationResponse(
                            LocalDate.parse("2025-06-30"),
                            LocalDate.parse("2025-07-02"),
                            5,
                            Collections.emptyList());

            when(managerServiceClient.findMonthlyReservation(anyLong(), anyString()))
                    .thenReturn(monthlyReservationResponse);

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
        }

        @Test
        void 모든_시간이_가득_찬_날짜는_예약_불가하다() {
            // given
            String date = "2025-07";
            LocalDate targetDate = LocalDate.of(2025, 7, 1);

            stubMonthlyReservation(date);
            stubAllTimeSlotsAreFullByCount(targetDate, 3);

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
        }

        @Test
        void 일부_시간만_예약된_날짜는_정확하게_표시된다() {
            // given
            String date = "2025-07";
            LocalDate targetDate = LocalDate.of(2025, 7, 2);
            int capacity = 3;

            stubMonthlyReservation(date);
            stubPartiallyReservedTimeSlots(targetDate, capacity);

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
                    assertThat(timeSlot.isPossible()).isFalse(); // 이미 예약됨
                } else {
                    assertThat(timeSlot.isPossible()).isTrue(); // 예약 가능
                }
            }
        }

        @Test
        void 예약_기간_아닌경우_빈_리스트_반환한다() {
            // given
            String date = "2025-08";

            when(managerServiceClient.findMonthlyReservation(anyLong(), anyString()))
                    .thenReturn(
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
        void 존재하지_않는_팝업ID는_예외처리_된다() {
            // given
            Long popupId = 999L;
            String date = "2025-07";

            when(managerServiceClient.findMonthlyReservation(any(), anyString()))
                    .thenThrow(new RuntimeException("해당 팝업이 존재하지 않습니다."));

            // when & then
            assertThatThrownBy(() -> memberReservationService.findAvailableDate(popupId, date))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("해당 팝업이 존재하지 않습니다.");
        }
    }

    private void stubMonthlyReservation(String date) {
        when(managerServiceClient.findMonthlyReservation(eq(popupId), eq(date)))
                .thenReturn(
                        new MonthlyReservationResponse(
                                LocalDate.of(2025, 6, 30),
                                LocalDate.of(2025, 7, 2),
                                3,
                                List.of(
                                        new DailyReservation(
                                                LocalDate.of(2025, 7, 1),
                                                List.of(
                                                        new TimeSlot(1L, LocalTime.of(12, 0)),
                                                        new TimeSlot(2L, LocalTime.of(13, 0)),
                                                        new TimeSlot(3L, LocalTime.of(14, 0)))),
                                        new DailyReservation(
                                                LocalDate.of(2025, 7, 2),
                                                List.of(
                                                        new TimeSlot(4L, LocalTime.of(12, 0)),
                                                        new TimeSlot(5L, LocalTime.of(13, 0)),
                                                        new TimeSlot(6L, LocalTime.of(14, 0)))))));
    }

    private void stubAllTimeSlotsAreFullByCount(LocalDate targetDate, int count) {
        List<HourlyReservationCount> hourlyCounts =
                List.of(
                        new HourlyReservationCount(LocalTime.of(12, 0), count),
                        new HourlyReservationCount(LocalTime.of(13, 0), count),
                        new HourlyReservationCount(LocalTime.of(14, 0), count));

        List<DailyReservationCountResponse> countResponseList =
                List.of(new DailyReservationCountResponse(targetDate, hourlyCounts));

        when(memberReservationRepository.findDailyReservationCount(
                        eq(popupId),
                        eq(LocalDate.of(2025, 6, 30)),
                        eq(LocalDate.of(2025, 7, 2)),
                        eq("2025-07")))
                .thenReturn(countResponseList);
    }

    private void stubPartiallyReservedTimeSlots(LocalDate date, int count) {
        List<HourlyReservationCount> hourlyCounts =
                List.of(
                        new HourlyReservationCount(LocalTime.of(12, 0), 0), // 가능
                        new HourlyReservationCount(LocalTime.of(13, 0), count), // 불가능
                        new HourlyReservationCount(LocalTime.of(14, 0), 0) // 가능
                        );

        List<DailyReservationCountResponse> countResponseList =
                List.of(new DailyReservationCountResponse(date, hourlyCounts));

        when(memberReservationRepository.findDailyReservationCount(
                        eq(popupId),
                        eq(LocalDate.of(2025, 6, 30)),
                        eq(LocalDate.of(2025, 7, 2)),
                        eq("2025-07")))
                .thenReturn(countResponseList);
    }

    @Nested
    class 설문지_조회할_때 {

        @Test
        void 팝업이_존재하면_정상적으로_조회된다() {
            // given
            stubSurveyChoices();

            // when
            List<SurveyChoiceResponse> choices =
                    memberReservationService.findSurveyChoicesByPopupId(popupId);

            assertThat(choices).hasSize(4);

            assertSurveyChoice(choices.get(0), 1L, 1L);
            assertSurveyChoice(choices.get(1), 2L, 6L);
            assertSurveyChoice(choices.get(2), 3L, 11L);
            assertSurveyChoice(choices.get(3), 4L, 16L);
        }
    }

    private void stubSurveyChoices() {
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

        when(managerServiceClient.findSurveyChoicesByPopupId(anyLong())).thenReturn(surveyChoices);
    }

    private void assertSurveyChoice(
            SurveyChoiceResponse response, Long expectedSurveyId, Long startingChoiceId) {

        assertThat(response.surveyId()).isEqualTo(expectedSurveyId);

        List<SurveyOption> options = response.options();
        assertThat(options).hasSize(5);

        for (int i = 0; i < options.size(); i++) {
            SurveyOption option = options.get(i);
            Long expectedChoiceId = startingChoiceId + i;
            String expectedContent = "보기" + (i + 1);

            assertThat(option.choiceId()).isEqualTo(expectedChoiceId);
            assertThat(option.content()).isEqualTo(expectedContent);
            assertThat(option.choiceId()).isGreaterThan(0L);
            assertThat(option.content()).isNotBlank();
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
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 설문지_응답이_4개면_producer_로_전송한다() {
            // given
            List<SurveyChoiceRequest> validChoices =
                    List.of(
                            new SurveyChoiceRequest(1L, 1L),
                            new SurveyChoiceRequest(2L, 2L),
                            new SurveyChoiceRequest(3L, 3L),
                            new SurveyChoiceRequest(4L, 4L));

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

            when(memberReservationRepository.findByMemberIdAndStatus(anyLong(), eq(RESERVED)))
                    .thenReturn(List.of(reservation));

            when(managerServiceClient.findReservedPopupInfoList(any()))
                    .thenReturn(
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
        }

        @Test
        void 대기중인_예약만_존재하면_빈_리스트를_반환한다() {
            // given
            when(memberReservationRepository.findByMemberIdAndStatus(anyLong(), eq(RESERVED)))
                    .thenReturn(Collections.emptyList());

            // when
            List<ReservationDetailResponse> result =
                    memberReservationService.findReservationInfo(memberId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void 예약_내역이_존재하지_않으면_빈_리스트를_반환한다() {
            // given
            when(memberReservationRepository.findByMemberIdAndStatus(anyLong(), eq(RESERVED)))
                    .thenReturn(Collections.emptyList());

            // when
            List<ReservationDetailResponse> result =
                    memberReservationService.findReservationInfo(memberId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void 예약_날짜_및_시간이_오늘보다_이전인_경우_빈_리스트를_반환한다() {
            // given
            when(memberReservationRepository.findByMemberIdAndStatus(anyLong(), eq(RESERVED)))
                    .thenReturn(Collections.emptyList());

            // when
            List<ReservationDetailResponse> result =
                    memberReservationService.findReservationInfo(memberId);

            // then
            assertThat(result).isEmpty();
        }
    }

    // TODO 인기 팝업 조회할 때

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

            when(memberReservationRepository.findUpcomingReservation(any()))
                    .thenReturn(reservation);

            when(managerServiceClient.findReservedPopupInfo(any()))
                    .thenReturn(
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
        }

        @Test
        void 예약했던_목록들이_현재_시간보다_이전이면_NULL을_반환한다() {
            when(memberReservationRepository.findUpcomingReservation(anyLong())).thenReturn(null);
            assertUpcomingReservationIsNull();
        }

        @Test
        void 예약_정보가_존재하지_않으면_NULL을_반환한다() {
            when(memberReservationRepository.findUpcomingReservation(any())).thenReturn(null);
            assertUpcomingReservationIsNull();
        }

        @Test
        void 현재_시간_이후_대기중인_예약이_존재하면_NULL을_반환한다() {
            when(memberReservationRepository.findUpcomingReservation(anyLong())).thenReturn(null);
            assertUpcomingReservationIsNull();
        }
    }

    private void assertPopupDate(AvailableDateResponse response) {
        org.junit.jupiter.api.Assertions.assertAll(
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
