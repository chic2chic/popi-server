package com.lgcns.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.lgcns.exception.MemberReservationErrorCode.RESERVATION_FAILED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.DatabaseCleaner;
import com.lgcns.WireMockIntegrationTest;
import com.lgcns.client.managerClient.dto.request.PopupIdsRequest;
import com.lgcns.domain.MemberReservation;
import com.lgcns.domain.MemberReservationStatus;
import com.lgcns.dto.request.SurveyChoiceRequest;
import com.lgcns.dto.response.*;
import com.lgcns.error.exception.CustomException;
import com.lgcns.error.feign.FeignErrorCode;
import com.lgcns.exception.MemberReservationErrorCode;
import com.lgcns.repository.MemberReservationRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;

class MemberReservationServiceTest extends WireMockIntegrationTest {

    @Autowired private MemberReservationService memberReservationService;
    @Autowired private MemberReservationRepository memberReservationRepository;
    @Autowired private RedisTemplate<String, String> redisTemplate;
    @Autowired private DatabaseCleaner databaseCleaner;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String memberId = "1";
    private final Long popupId = 1L;
    private final Long reservationId = 1L;

    private final AtomicLong reservationIdGenerator = new AtomicLong(1);
    private final AtomicLong memberIdGenerator = new AtomicLong(1);

    @BeforeEach
    void cleanDatabase() {
        databaseCleaner.execute();
    }

    @BeforeEach
    void injectMockito() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    class 예약_가능한_날짜를_조회할_때 {

        @Test
        void 날짜에_문자가_포함되면_예외가_발생한다() {
            // given
            String date = "2025-June";

            // when & then
            assertThatThrownBy(() -> memberReservationService.findAvailableDate(popupId, date))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode", MemberReservationErrorCode.INVALID_DATE_FORMAT);
        }

        @Test
        void 날짜_형식이_yyyy_MM_아니면_예외가_발생한다() {
            // given
            String date = "2025-5";

            // when & then
            assertThatThrownBy(() -> memberReservationService.findAvailableDate(popupId, date))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode", MemberReservationErrorCode.INVALID_DATE_FORMAT);
        }

        @Test
        void 월이_1부터_12가_아니면_예외가_발생한다() {
            // given
            String date = "2025-13";

            // when & then
            assertThatThrownBy(() -> memberReservationService.findAvailableDate(popupId, date))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode", MemberReservationErrorCode.INVALID_DATE_FORMAT);
        }

        @Test
        void 예약이_없는_경우_모든_시간이_예약_가능하다() throws JsonProcessingException {
            // given
            String date = "2025-05";

            String expectedResponse =
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "popupOpenDate",
                                    "2025-05-31",
                                    "popupCloseDate",
                                    "2025-06-02",
                                    "timeCapacity",
                                    5,
                                    "dailyReservations",
                                    List.of()));

            stubFindAvailableDate(popupId, date, 200, expectedResponse);

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
        void 모든_시간이_가득_찬_날짜는_예약_불가하다() throws JsonProcessingException {
            // given
            String date = "2025-06";

            createMemberReservation();

            String expectedResponse =
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "popupOpenDate",
                                    "2025-05-31",
                                    "popupCloseDate",
                                    "2025-06-02",
                                    "timeCapacity",
                                    5,
                                    "dailyReservations",
                                    List.of(
                                            Map.of(
                                                    "reservationDate",
                                                    "2025-06-01",
                                                    "timeSlots",
                                                    List.of(
                                                            Map.of(
                                                                    "reservationId",
                                                                    1,
                                                                    "time",
                                                                    "12:00"),
                                                            Map.of(
                                                                    "reservationId",
                                                                    2,
                                                                    "time",
                                                                    "13:00"),
                                                            Map.of(
                                                                    "reservationId",
                                                                    3,
                                                                    "time",
                                                                    "14:00"))),
                                            Map.of(
                                                    "reservationDate",
                                                    "2025-06-02",
                                                    "timeSlots",
                                                    List.of(
                                                            Map.of(
                                                                    "reservationId",
                                                                    4,
                                                                    "time",
                                                                    "12:00"),
                                                            Map.of(
                                                                    "reservationId",
                                                                    5,
                                                                    "time",
                                                                    "13:00"),
                                                            Map.of(
                                                                    "reservationId",
                                                                    6,
                                                                    "time",
                                                                    "14:00"))))));

            stubFindAvailableDate(popupId, date, 200, expectedResponse);

            // when
            AvailableDateResponse response =
                    memberReservationService.findAvailableDate(popupId, date);

            ReservableDate reservableDate =
                    response.reservableDate().stream()
                            .filter(d -> d.date().equals(LocalDate.of(2025, 6, 1)))
                            .findFirst()
                            .orElseThrow();

            // then
            assertPopupDate(response);
            assertThat(reservableDate.isReservable()).isFalse();

            for (ReservableTime reservableTime : reservableDate.timeSlots()) {
                assertThat(reservableTime.isPossible()).isFalse(); // 모두 불가해야 함
            }
        }

        @Test
        void 일부_시간만_예약된_날짜는_정확하게_표시된다() throws JsonProcessingException {
            // given
            String date = "2025-06";

            createMemberReservation();

            String expectedResponse =
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "popupOpenDate",
                                    "2025-05-31",
                                    "popupCloseDate",
                                    "2025-06-02",
                                    "timeCapacity",
                                    5,
                                    "dailyReservations",
                                    List.of(
                                            Map.of(
                                                    "reservationDate",
                                                    "2025-06-01",
                                                    "timeSlots",
                                                    List.of(
                                                            Map.of(
                                                                    "reservationId",
                                                                    4,
                                                                    "time",
                                                                    "12:00"),
                                                            Map.of(
                                                                    "reservationId",
                                                                    5,
                                                                    "time",
                                                                    "13:00"),
                                                            Map.of(
                                                                    "reservationId",
                                                                    6,
                                                                    "time",
                                                                    "14:00"))),
                                            Map.of(
                                                    "reservationDate",
                                                    "2025-06-02",
                                                    "timeSlots",
                                                    List.of(
                                                            Map.of(
                                                                    "reservationId",
                                                                    7,
                                                                    "time",
                                                                    "12:00"),
                                                            Map.of(
                                                                    "reservationId",
                                                                    8,
                                                                    "time",
                                                                    "13:00"),
                                                            Map.of(
                                                                    "reservationId",
                                                                    9,
                                                                    "time",
                                                                    "14:00"))))));

            stubFindAvailableDate(popupId, date, 200, expectedResponse);

            // when
            AvailableDateResponse response =
                    memberReservationService.findAvailableDate(popupId, date);

            ReservableDate reservableDate =
                    response.reservableDate().stream()
                            .filter(d -> d.date().equals(LocalDate.of(2025, 6, 2)))
                            .findFirst()
                            .orElseThrow();

            // then
            assertPopupDate(response);
            assertThat(reservableDate.isReservable()).isTrue();

            for (ReservableTime reservableTime : reservableDate.timeSlots()) {
                if (reservableTime.time().equals(LocalTime.of(13, 0))) {
                    assertThat(reservableTime.isPossible()).isFalse(); // 예약 불가
                } else {
                    assertThat(reservableTime.isPossible()).isTrue(); // 예약 가능
                }
            }
        }

        @Test
        void 예약_기간_아닌경우_빈_리스트_반환한다() throws JsonProcessingException {
            // given
            String date = "2025-07";

            String expectedResponse =
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "popupOpenDate",
                                    "2025-05-31",
                                    "popupCloseDate",
                                    "2025-06-02",
                                    "timeCapacity",
                                    5,
                                    "dailyReservations",
                                    List.of()));

            stubFindAvailableDate(popupId, date, 200, expectedResponse);

            // when
            AvailableDateResponse response =
                    memberReservationService.findAvailableDate(popupId, date);

            // then
            assertPopupDate(response);
            assertThat(response.reservableDate()).isEmpty();
        }

        @Test
        void 존재하지_않는_팝업ID는_예외처리_된다() throws JsonProcessingException {
            // given
            Long invalidPopupId = 999L;
            String date = "2025-06";

            String expectedResponse =
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "success",
                                    false,
                                    "status",
                                    404,
                                    "data",
                                    Map.of(
                                            "errorClassName", "POPUP_NOT_FOUND",
                                            "message", "해당 팝업이 존재하지 않습니다."),
                                    "timestamp",
                                    "2025-05-23T01:50:46.229657"));

            stubFindAvailableDate(invalidPopupId, date, 404, expectedResponse);

            // when & then
            assertThatThrownBy(
                            () -> memberReservationService.findAvailableDate(invalidPopupId, date))
                    .isInstanceOf(CustomException.class)
                    .satisfies(
                            ex -> {
                                CustomException e = (CustomException) ex;
                                assertThat(e.getErrorCode().getErrorName())
                                        .isEqualTo("POPUP_NOT_FOUND");
                                assertThat(e.getErrorCode().getMessage())
                                        .isEqualTo("해당 팝업이 존재하지 않습니다.");
                            });
        }
    }

    @Nested
    class 설문지_조회할_때 {

        @Test
        void 팝업이_존재하면_조회에_성공한다() throws JsonProcessingException {

            // given
            String expectedResponse =
                    objectMapper.writeValueAsString(
                            List.of(
                                    Map.of(
                                            "surveyId",
                                            1,
                                            "options",
                                            List.of(
                                                    Map.of("choiceId", 1, "content", "보기1"),
                                                    Map.of("choiceId", 2, "content", "보기2"),
                                                    Map.of("choiceId", 3, "content", "보기3"),
                                                    Map.of("choiceId", 4, "content", "보기4"),
                                                    Map.of("choiceId", 5, "content", "보기5"))),
                                    Map.of(
                                            "surveyId",
                                            2,
                                            "options",
                                            List.of(
                                                    Map.of("choiceId", 6, "content", "보기1"),
                                                    Map.of("choiceId", 7, "content", "보기2"),
                                                    Map.of("choiceId", 8, "content", "보기3"),
                                                    Map.of("choiceId", 9, "content", "보기4"),
                                                    Map.of("choiceId", 10, "content", "보기5"))),
                                    Map.of(
                                            "surveyId",
                                            3,
                                            "options",
                                            List.of(
                                                    Map.of("choiceId", 11, "content", "보기1"),
                                                    Map.of("choiceId", 12, "content", "보기2"),
                                                    Map.of("choiceId", 13, "content", "보기3"),
                                                    Map.of("choiceId", 14, "content", "보기4"),
                                                    Map.of("choiceId", 15, "content", "보기5"))),
                                    Map.of(
                                            "surveyId",
                                            4,
                                            "options",
                                            List.of(
                                                    Map.of("choiceId", 16, "content", "보기1"),
                                                    Map.of("choiceId", 17, "content", "보기2"),
                                                    Map.of("choiceId", 18, "content", "보기3"),
                                                    Map.of("choiceId", 19, "content", "보기4"),
                                                    Map.of("choiceId", 20, "content", "보기5")))));

            stubFindSurveyChoicesByPopupId(popupId, 200, expectedResponse);

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

    @Nested
    class 설문지_등록_할_때 {
        @Test
        void 설문지_응답이_정상적으로_저장된다() throws JsonProcessingException {
            // given
            List<SurveyChoiceRequest> surveyChoices =
                    List.of(
                            new SurveyChoiceRequest(1L, 1L),
                            new SurveyChoiceRequest(2L, 5L),
                            new SurveyChoiceRequest(3L, 9L),
                            new SurveyChoiceRequest(4L, 13L));

            // when
            memberReservationService.createMemberAnswer(popupId, memberId, surveyChoices);

            // then
            // TODO kafka를 통해 응답이 잘 저장되었는지 검증하는 테스트 코드 필요
        }

        @Test
        void 설문지_응답이_4개가_아니면_예외가_발생한다() {
            // given
            List<SurveyChoiceRequest> surveyChoices =
                    List.of(
                            new SurveyChoiceRequest(1L, 1L),
                            new SurveyChoiceRequest(2L, 2L),
                            new SurveyChoiceRequest(3L, 3L));

            // when & then
            assertThatThrownBy(
                            () ->
                                    memberReservationService.createMemberAnswer(
                                            popupId, memberId, surveyChoices))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(
                            MemberReservationErrorCode.INVALID_SURVEY_CHOICES_COUNT.getMessage());
        }
    }

    @Nested
    class 회원이_예약_생성_할_때 {

        @Test
        void 예약이_존재하고_예약_가능한_상태이면_예약에_성공한다() throws JsonProcessingException {
            // given
            redisTemplate.opsForValue().set(reservationId.toString(), "10");

            // when
            memberReservationService.createMemberReservation(memberId, reservationId);

            // then

            Assertions.assertAll(
                    () ->
                            assertThat(
                                            memberReservationRepository
                                                    .existsMemberReservationByMemberIdAndReservationId(
                                                            Long.parseLong(memberId),
                                                            reservationId))
                                    .isTrue(),
                    () ->
                            assertThat(redisTemplate.opsForValue().get(reservationId.toString()))
                                    .isEqualTo("9"));

            redisTemplate.delete(reservationId.toString());
        }

        @Test
        void 이미_예약한_사용자는_예외가_발생한다() throws JsonProcessingException {
            // given
            redisTemplate.opsForValue().set(reservationId.toString(), "10");

            memberReservationRepository.save(
                    MemberReservation.createMemberReservation(
                            reservationId, Long.parseLong(memberId)));

            // when & then
            assertThatThrownBy(
                            () ->
                                    memberReservationService.createMemberReservation(
                                            memberId, reservationId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(
                            MemberReservationErrorCode.RESERVATION_ALREADY_EXISTS.getMessage());

            redisTemplate.delete(reservationId.toString());
        }

        @Test
        void 예약가능수량이_없으면_예약에_실패하고_Redis_복구가_일어난다() throws JsonProcessingException {
            // given
            Long reservationId = 1L;
            redisTemplate.opsForValue().set(reservationId.toString(), "0");

            // when & then
            assertThatThrownBy(
                            () ->
                                    memberReservationService.createMemberReservation(
                                            memberId, reservationId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(RESERVATION_FAILED.getMessage());

            assertThat(redisTemplate.opsForValue().get(reservationId.toString())).isEqualTo("0");
            redisTemplate.delete(reservationId.toString());
        }
    }

    @Nested
    class 회원의_예약_정보를_업데이트할_때 {

        @Test
        void 예약이_존재하고_업데이트_가능한_상태이면_예약_정보를_업데이트한다() throws JsonProcessingException {
            // given
            MemberReservation memberReservation =
                    MemberReservation.createMemberReservation(
                            Long.parseLong(memberId), reservationId);
            memberReservationRepository.save(memberReservation);

            stubForFindMemberInternalInfo(
                    memberId,
                    200,
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "memberId",
                                    memberId,
                                    "nickname",
                                    "testUser",
                                    "age",
                                    "TWENTIES",
                                    "gender",
                                    "MALE",
                                    "role",
                                    "USER",
                                    "status",
                                    "NORMAL")));

            stubFindReservationById(
                    reservationId,
                    200,
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "reservationId",
                                    reservationId,
                                    "popupId",
                                    popupId,
                                    "reservationDate",
                                    "2025-06-01",
                                    "reservationTime",
                                    "12:00")));

            // when
            memberReservationService.updateMemberReservation(memberReservation.getId());

            // then
            MemberReservation updatedMemberReservation =
                    memberReservationRepository.findById(memberReservation.getId()).get();
            Assertions.assertAll(
                    () ->
                            assertThat(updatedMemberReservation.getStatus())
                                    .isEqualTo(MemberReservationStatus.RESERVED),
                    () ->
                            assertThat(updatedMemberReservation.getReservationDate())
                                    .isEqualTo(LocalDate.of(2025, 6, 1)),
                    () ->
                            assertThat(updatedMemberReservation.getReservationTime())
                                    .isEqualTo(LocalTime.of(12, 0)));

            redisTemplate.delete(reservationId.toString());
        }

        @Test
        void 회원_예약이_존재하지_않으면_예외가_발생한다() {
            // given
            Long invalidMemberReservationId = -1L;

            // when & then
            assertThatThrownBy(
                            () ->
                                    memberReservationService.updateMemberReservation(
                                            invalidMemberReservationId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(
                            MemberReservationErrorCode.MEMBER_RESERVATION_NOT_FOUND.getMessage());
        }

        @Test
        void FEIGN_CLIENT_API_호출_실패시_예외가_발생한다() throws JsonProcessingException {
            // given
            MemberReservation memberReservation =
                    MemberReservation.createMemberReservation(
                            Long.parseLong(memberId), reservationId);
            memberReservationRepository.save(memberReservation);

            stubForFindMemberInternalInfo(
                    memberId,
                    200,
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "memberId",
                                    memberId,
                                    "nickname",
                                    "testUser",
                                    "age",
                                    "TWENTIES",
                                    "gender",
                                    "MALE",
                                    "role",
                                    "USER",
                                    "status",
                                    "NORMAL")));

            stubFindReservationById(reservationId, 500, "");

            // when & then
            assertThatThrownBy(
                            () ->
                                    memberReservationService.updateMemberReservation(
                                            memberReservation.getId()))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Feign 예외 디코딩 실패");
        }

        @Test
        void 예약이_존재하지_않으면_예외가_발생한다() throws JsonProcessingException {
            // given
            MemberReservation memberReservation =
                    MemberReservation.createMemberReservation(
                            Long.parseLong(memberId), reservationId);
            memberReservationRepository.save(memberReservation);

            stubForFindMemberInternalInfo(
                    memberId,
                    200,
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "memberId",
                                    memberId,
                                    "nickname",
                                    "testUser",
                                    "age",
                                    "TWENTIES",
                                    "gender",
                                    "MALE",
                                    "role",
                                    "USER",
                                    "status",
                                    "NORMAL")));

            stubFindReservationById(
                    reservationId,
                    404,
                    """
                            {
                              "success": false,
                              "status": 404,
                              "data": {
                                "errorClassName": "RESERVATION_NOT_FOUND",
                                "message": "해당 예약을 찾을 수 없습니다."
                              },
                              "timestamp": "2025-05-27T17:24:45.082753"
                            }
                            """);

            // when & then
            assertThatThrownBy(
                            () ->
                                    memberReservationService.updateMemberReservation(
                                            memberReservation.getId()))
                    .isInstanceOf(CustomException.class)
                    .satisfies(
                            ex -> {
                                CustomException ce = (CustomException) ex;
                                assertThat(ce.getErrorCode()).isInstanceOf(FeignErrorCode.class);

                                FeignErrorCode errorCode = (FeignErrorCode) ce.getErrorCode();
                                assertThat(errorCode.getErrorName())
                                        .isEqualTo("RESERVATION_NOT_FOUND");
                                assertThat(errorCode.getMessage()).contains("해당 예약을 찾을 수 없습니다.");
                            });
        }
    }

    @Nested
    class 회원의_예약이_취소될_때 {

        @Test
        void 예약이_존재하면_예약을_취소한다() throws JsonProcessingException {
            // given
            redisTemplate.opsForValue().set(reservationId.toString(), "10");
            MemberReservation memberReservation =
                    MemberReservation.createMemberReservation(
                            Long.parseLong(memberId), reservationId);
            memberReservationRepository.save(memberReservation);

            // when
            memberReservationService.cancelMemberReservation(memberReservation.getId());

            // then

            Optional<MemberReservation> optionalMemberReservation =
                    memberReservationRepository.findById(memberReservation.getId());
            Assertions.assertAll(
                    () -> assertThat(optionalMemberReservation).isEmpty(),
                    () ->
                            assertThat(redisTemplate.opsForValue().get(reservationId.toString()))
                                    .isEqualTo("11"));

            redisTemplate.delete(reservationId.toString());
        }

        @Test
        void 예약이_존재하지_않으면_예외가_발생한다() {
            // given
            Long invalidMemberReservationId = -1L;

            // when & then
            assertThatThrownBy(
                            () ->
                                    memberReservationService.cancelMemberReservation(
                                            invalidMemberReservationId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(
                            MemberReservationErrorCode.MEMBER_RESERVATION_NOT_FOUND.getMessage());
        }
    }

    @Nested
    class 예약_목록_조회할_때 {

        @Test
        void 완료된_예약이_존재하면_조회에_성공한다() throws JsonProcessingException {
            // given
            PopupIdsRequest request = PopupIdsRequest.of(List.of(popupId));

            insertReservedMemberReservation(LocalDate.of(2025, 6, 1), LocalTime.of(12, 0));

            String expectedResponse =
                    objectMapper.writeValueAsString(
                            List.of(
                                    Map.of(
                                            "popupId",
                                            1,
                                            "popupName",
                                            "BLACK PINK 팝업스토어",
                                            "address",
                                            "서울특별시 영등포구 여의대로 108, 5층",
                                            "latitude",
                                            37.527097,
                                            "longitude",
                                            126.927301)));

            stubFindReservationInfoList(request, 200, expectedResponse);

            // when
            List<ReservationDetailResponse> reservations =
                    memberReservationService.findReservationInfo(memberId);

            // then
            assertThat(reservations).isNotEmpty();
            assertThat(reservations.size()).isEqualTo(1);

            ReservationDetailResponse reservationInfo = reservations.get(0);

            Assertions.assertAll(
                    () -> assertThat(reservations).isNotEmpty(),
                    () -> assertThat(reservations.size()).isEqualTo(1),
                    () -> assertThat(reservationInfo.popupId()).isEqualTo(1L),
                    () -> assertThat(reservationInfo.popupName()).isEqualTo("BLACK PINK 팝업스토어"),
                    () -> assertThat(reservationInfo.reservationTime()).isEqualTo("12:00"),
                    () -> assertThat(reservationInfo.reservationDay()).isEqualTo("SUN"),
                    () ->
                            assertThat(reservationInfo.address())
                                    .isEqualTo("서울특별시 영등포구 여의대로 108, 5층"),
                    () -> assertThat(reservationInfo.latitude()).isEqualTo(37.527097),
                    () -> assertThat(reservationInfo.longitude()).isEqualTo(126.927301),
                    () -> assertThat(reservationInfo.qrImage()).isEqualTo("iVBORw0KGgoAAAA..."));
        }

        @Test
        void 대기중인_예약만_존재하면_빈_리스트를_반환한다() {
            // given
            insertPendingMemberReservation(LocalDate.now().plusDays(1), LocalTime.of(12, 0));

            // when
            List<ReservationDetailResponse> reservations =
                    memberReservationService.findReservationInfo(memberId);

            // then
            assertThat(reservations).isEmpty();
        }

        @Test
        void 예약_내역이_존재하지_않으면_빈_리스트를_반환한다() {
            // given
            String memberId = "99";

            // when
            List<ReservationDetailResponse> reservations =
                    memberReservationService.findReservationInfo(memberId);

            // then
            assertThat(reservations).isEmpty();
        }

        @Test
        void 예약_날짜_및_시간이_오늘보다_이전인_경우_빈_리스트를_반환한다() {
            // given
            insertReservedMemberReservation(LocalDate.now().minusDays(1), LocalTime.of(12, 0));

            // when
            List<ReservationDetailResponse> reservations =
                    memberReservationService.findReservationInfo(memberId);

            // then
            assertThat(reservations).isEmpty();
        }
    }

    @Nested
    class 인기_팝업_아이디를_조회할_때 {

        @Test
        void 예약된_팝업이_4개_이상이면_크기가_4인_리스트를_예약자_수_내림차순으로_반환한다() {
            // given
            memberReservationRepository.deleteAll();

            insertMultipleReservations(1L, 10);
            insertMultipleReservations(2L, 8);
            insertMultipleReservations(3L, 6);
            insertMultipleReservations(4L, 4);
            insertMultipleReservations(5L, 2);

            // when
            List<Long> expectedIds = memberReservationService.findHotPopupIds();

            // then
            assertThat(expectedIds).hasSize(4);
            assertThat(expectedIds).containsExactly(1L, 2L, 3L, 4L);
        }

        @Test
        void 예약자_수가_모두_같은_경우_팝업_아이디_오름차순으로_반환한다() {
            // given
            memberReservationRepository.deleteAll();

            insertMultipleReservations(3L, 5);
            insertMultipleReservations(1L, 5);
            insertMultipleReservations(4L, 5);
            insertMultipleReservations(2L, 5);

            // when
            List<Long> expectedIds = memberReservationService.findHotPopupIds();

            // then
            assertThat(expectedIds).hasSize(4);
            assertThat(expectedIds).containsExactly(1L, 2L, 3L, 4L); // 팝업 ID 오름차순
        }

        @Test
        void 예약된_팝업이_2개인_경우_크키가_2인_리스트를_예약자_수_내림차순으로_반환한다() {
            // given
            memberReservationRepository.deleteAll();

            insertMultipleReservations(1L, 5);
            insertMultipleReservations(2L, 7);

            // when
            List<Long> expectedIds = memberReservationService.findHotPopupIds();

            // then
            assertThat(expectedIds).hasSize(2);
            assertThat(expectedIds).containsExactly(2L, 1L);
        }

        @Test
        void 예약된_팝없이_없는_경우_빈_리스트를_반환한다() {
            // given
            memberReservationRepository.deleteAll();

            // when
            List<Long> expectedIds = memberReservationService.findHotPopupIds();

            // then
            assertThat(expectedIds).isEmpty();
        }
    }

    @Nested
    class 가장_가까운_예약_조회할_때 {

        @Test
        void 현재_시간_이후의_완료된_예약이_존재하면_가장_가까운_예약_조회에_성공한다() throws JsonProcessingException {
            // given
            LocalDate now = LocalDate.now();
            insertReservedMemberReservation(now.plusDays(1), LocalTime.of(12, 0));

            String expectedResponse =
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "popupId",
                                    1,
                                    "popupName",
                                    "BLACK PINK 팝업스토어",
                                    "address",
                                    "서울특별시 영등포구 여의대로 108, 5층",
                                    "latitude",
                                    37.527097,
                                    "longitude",
                                    126.927301));

            stubFindReservationInfo(popupId, 200, expectedResponse);

            // when
            ReservationDetailResponse response =
                    memberReservationService.findUpcomingReservationInfo(memberId);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.popupId()).isEqualTo(1L),
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
                    () -> assertThat(response.address()).isEqualTo("서울특별시 영등포구 여의대로 108, 5층"),
                    () -> assertThat(response.latitude()).isEqualTo(37.527097),
                    () -> assertThat(response.longitude()).isEqualTo(126.927301),
                    () -> assertThat(response.qrImage()).isEqualTo("iVBORw0KGgoAAAA..."));
        }

        @Test
        void 예약했던_목록들이_현재_시간보다_이전이면_NULL을_반환한다() throws JsonProcessingException {
            // given
            insertReservedMemberReservation(LocalDate.now().minusDays(1), LocalTime.of(12, 0));

            String expectedResponse = objectMapper.writeValueAsString(null);

            stubFindReservationInfo(popupId, 200, expectedResponse);

            // when
            ReservationDetailResponse response =
                    memberReservationService.findUpcomingReservationInfo(memberId);

            // then
            assertThat(response).isEqualTo(null);
        }

        @Test
        void 예약_정보가_존재하지_않으면_NULL을_반환한다() {
            // given & when
            ReservationDetailResponse response =
                    memberReservationService.findUpcomingReservationInfo(memberId);

            // then
            assertThat(response).isEqualTo(null);
        }

        @Test
        void 현재_시간_이후_대기중인_예약이_존재하면_NULL을_반환한다() throws JsonProcessingException {
            // given
            LocalDate now = LocalDate.now();
            insertPendingMemberReservation(now.plusDays(1), LocalTime.of(12, 0));

            String expectedResponse = objectMapper.writeValueAsString(null);

            stubFindReservationInfo(popupId, 200, expectedResponse);

            // when
            ReservationDetailResponse response =
                    memberReservationService.findUpcomingReservationInfo(memberId);

            // then
            assertThat(response).isEqualTo(null);
        }
    }

    @Nested
    class 오늘_예약자_수를_조회할_때 {

        @Test
        void 예약자가_있는_경우_조회에_성공한다() {
            // given
            LocalDate now = LocalDate.now();
            insertReservedMemberReservation(now, LocalTime.of(12, 0));

            // when
            DailyMemberReservationCountResponse response =
                    memberReservationService.findDailyMemberReservationCount(popupId);

            // then
            Assertions.assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.reservationCount()).isEqualTo(5));
        }

        @Test
        void 예약자가_없는_경우_조회에_성공한다() {
            // given & when
            DailyMemberReservationCountResponse response =
                    memberReservationService.findDailyMemberReservationCount(popupId);

            // then
            Assertions.assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.reservationCount()).isEqualTo(0));
        }
    }

    private void insertMultipleReservations(Long popupId, int count) {
        for (int i = 0; i < count; i++) {
            MemberReservation reservation =
                    MemberReservation.builder()
                            .reservationId(reservationIdGenerator.getAndIncrement())
                            .memberId(memberIdGenerator.getAndIncrement())
                            .popupId(popupId)
                            .qrImage("iVBORw0KGgoAAAA...")
                            .reservationDate(LocalDate.of(2025, 6, 1))
                            .reservationTime(LocalTime.of(12, 0))
                            .build();
            memberReservationRepository.save(reservation);
        }
    }

    private void stubFindAvailableDate(Long popupId, String date, int status, String body) {

        wireMockServer.stubFor(
                get(urlPathEqualTo("/internal/reservations/popups/" + popupId))
                        .withQueryParam("date", equalTo(date))
                        .willReturn(
                                aResponse()
                                        .withStatus(status)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(body)));
    }

    private void stubFindSurveyChoicesByPopupId(Long popupId, int status, String body) {
        wireMockServer.stubFor(
                get(urlPathEqualTo("/internal/reservations/popups/" + popupId + "/survey"))
                        .willReturn(
                                aResponse()
                                        .withStatus(status)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(body)));
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

    private void stubFindReservationById(Long reservationId, int status, String body) {
        try {
            wireMockServer.stubFor(
                    get(urlPathEqualTo("/internal/reservations/" + reservationId))
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

    private void stubFindReservationInfoList(PopupIdsRequest request, int status, String body)
            throws JsonProcessingException {
        wireMockServer.stubFor(
                post(urlPathEqualTo("/internal/reservations"))
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(request)))
                        .willReturn(
                                aResponse()
                                        .withStatus(status)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(body)));
    }

    private void stubFindReservationInfo(Long popupId, int status, String body) {
        wireMockServer.stubFor(
                get(urlPathEqualTo("/internal/popups" + "/" + popupId))
                        .willReturn(
                                aResponse()
                                        .withStatus(status)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(body)));
    }

    private void createMemberReservation() {
        // 6월 1일은 모두 찬 상태
        insertReservedMemberReservation(LocalDate.of(2025, 6, 1), LocalTime.of(12, 0));
        insertReservedMemberReservation(LocalDate.of(2025, 6, 1), LocalTime.of(13, 0));
        insertReservedMemberReservation(LocalDate.of(2025, 6, 1), LocalTime.of(14, 0));

        // 6월 2일은 일부만 찬 상태
        insertReservedMemberReservation(LocalDate.of(2025, 6, 2), LocalTime.of(13, 0));
    }

    private void insertPendingMemberReservation(LocalDate date, LocalTime time) {
        for (int i = 0; i < 5; i++) {
            MemberReservation reservation =
                    MemberReservation.builder()
                            .reservationId(reservationIdGenerator.getAndIncrement())
                            .memberId(memberIdGenerator.getAndIncrement())
                            .popupId(popupId)
                            .qrImage("iVBORw0KGgoAAAA...")
                            .reservationDate(date)
                            .reservationTime(time)
                            .build();
            memberReservationRepository.save(reservation);
        }
    }

    private void insertReservedMemberReservation(LocalDate date, LocalTime time) {
        for (int i = 0; i < 5; i++) {
            MemberReservation reservation =
                    MemberReservation.builder()
                            .reservationId(reservationIdGenerator.getAndIncrement())
                            .memberId(memberIdGenerator.getAndIncrement())
                            .popupId(popupId)
                            .qrImage("iVBORw0KGgoAAAA...")
                            .reservationDate(date)
                            .reservationTime(time)
                            .build();

            reservation.updateMemberReservation(
                    reservation.getPopupId(),
                    reservation.getQrImage(),
                    reservation.getReservationDate(),
                    reservation.getReservationTime());

            memberReservationRepository.save(reservation);
        }
    }

    private void assertPopupDate(AvailableDateResponse response) {
        Assertions.assertAll(
                () -> assertThat(response.popupOpenDate()).isEqualTo("2025-05-31"),
                () -> assertThat(response.popupCloseDate()).isEqualTo("2025-06-02"));
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
}
