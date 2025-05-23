package com.lgcns.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.WireMockIntegrationTest;
import com.lgcns.domain.MemberReservation;
import com.lgcns.dto.response.AvailableDateResponse;
import com.lgcns.dto.response.ReservableDate;
import com.lgcns.dto.response.ReservableTime;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.MemberReservationErrorCode;
import com.lgcns.repository.MemberReservationRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class MemberReservationServiceTest extends WireMockIntegrationTest {

    @Autowired private MemberReservationService memberReservationService;

    @Autowired private MemberReservationRepository memberReservationRepository;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String memberId = "1";
    private final Long popupId = 1L;
    private final AtomicLong reservationIdGenerator = new AtomicLong(4);
    private final AtomicLong memberIdGenerator = new AtomicLong(1);

    @BeforeEach
    void setUp() throws JsonProcessingException {
        // 6월 1일은 모두 찬 상태
        insertMemberReservation(LocalDate.of(2025, 6, 1), LocalTime.of(12, 0));
        insertMemberReservation(LocalDate.of(2025, 6, 1), LocalTime.of(13, 0));
        insertMemberReservation(LocalDate.of(2025, 6, 1), LocalTime.of(14, 0));

        // 6월 2일은 일부만 찬 상태
        insertMemberReservation(LocalDate.of(2025, 6, 2), LocalTime.of(13, 0));

        String fullyBooked =
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
                                                        Map.of("reservationId", 4, "time", "12:00"),
                                                        Map.of("reservationId", 5, "time", "13:00"),
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
                                                        Map.of("reservationId", 7, "time", "12:00"),
                                                        Map.of("reservationId", 8, "time", "13:00"),
                                                        Map.of(
                                                                "reservationId",
                                                                9,
                                                                "time",
                                                                "14:00"))))));

        String partiallyAvailable =
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
                                                "2025-05-31",
                                                "timeSlots",
                                                List.of(
                                                        Map.of("reservationId", 1, "time", "12:00"),
                                                        Map.of("reservationId", 2, "time", "13:00"),
                                                        Map.of(
                                                                "reservationId",
                                                                3,
                                                                "time",
                                                                "14:00"))))));

        String noReservations =
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

        String popupNotFound =
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

        stubFindAvailableDate(1L, "2025-05", 200, partiallyAvailable);
        stubFindAvailableDate(1L, "2025-06", 200, fullyBooked);
        stubFindAvailableDate(1L, "2025-07", 200, noReservations);
        stubFindAvailableDate(999L, "2025-06", 404, popupNotFound);
    }

    @Nested
    @DisplayName("예약 가능 날짜 조회")
    class FindAvailableDate {

        @Test
        void 날짜에_문자가_포함되면_예외발생() {
            // given
            String date = "2025-June";

            // when & then
            assertThatThrownBy(
                            () ->
                                    memberReservationService.findAvailableDate(
                                            memberId, popupId, date))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode", MemberReservationErrorCode.INVALID_DATE_FORMAT);
        }

        @Test
        void 날짜_형식이_yyyy_MM이_아니면_예외발생() {
            // given
            String date = "2025-5";

            // when & then
            assertThatThrownBy(
                            () ->
                                    memberReservationService.findAvailableDate(
                                            memberId, popupId, date))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode", MemberReservationErrorCode.INVALID_DATE_FORMAT);
        }

        @Test
        void 월이_1부터_12가_아니면_예외발생() {
            // given
            String date = "2025-13";

            // when & then
            assertThatThrownBy(
                            () ->
                                    memberReservationService.findAvailableDate(
                                            memberId, popupId, date))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode", MemberReservationErrorCode.INVALID_DATE_FORMAT);
        }

        @Test
        void 예약이_없는_경우_모든_시간_예약_가능() {
            // given
            String date = "2025-05";

            // when
            AvailableDateResponse response =
                    memberReservationService.findAvailableDate(memberId, popupId, date);

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
        void 모든_시간이_가득_찬_날짜는_예약불가() {
            // given
            String date = "2025-06";

            // when
            AvailableDateResponse response =
                    memberReservationService.findAvailableDate(memberId, popupId, date);

            ReservableDate fullyBooked =
                    response.reservableDate().stream()
                            .filter(d -> d.date().equals(LocalDate.of(2025, 6, 1)))
                            .findFirst()
                            .orElseThrow();

            // then
            assertPopupDate(response);
            assertThat(fullyBooked.isReservable()).isFalse();

            for (ReservableTime reservableTime : fullyBooked.timeSlots()) {
                assertThat(reservableTime.isPossible()).isFalse(); // 모두 불가해야 함
            }
        }

        @Test
        void 일부_시간만_예약된_날짜는_정확하게_표시된다() {
            // given
            String date = "2025-06";

            // when
            AvailableDateResponse response =
                    memberReservationService.findAvailableDate(memberId, popupId, date);

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
        void 예약_기간_아닌경우_빈_리스트_반환한다() {
            // given
            String date = "2025-07";

            // when
            AvailableDateResponse response =
                    memberReservationService.findAvailableDate(memberId, popupId, date);

            // then
            assertPopupDate(response);
            assertThat(response.reservableDate()).isEmpty();
        }

        @Test
        void 존재하지_않는_팝업ID는_예외처리_된다() {
            // given
            Long invalidPopupId = 999L;
            String date = "2025-06";

            // when & then
            assertThatThrownBy(
                            () ->
                                    memberReservationService.findAvailableDate(
                                            memberId, invalidPopupId, date))
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

    private void stubFindAvailableDate(Long popupId, String date, int status, String body) {
        try {
            wireMockServer.stubFor(
                    get(urlPathEqualTo("/internal/reservations/popups/" + popupId))
                            .withQueryParam("date", equalTo(date))
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

    private void insertMemberReservation(LocalDate date, LocalTime time) {
        for (int i = 0; i < 5; i++) {
            MemberReservation reservation =
                    MemberReservation.createMemberReservation(
                            reservationIdGenerator.getAndIncrement(),
                            memberIdGenerator.getAndIncrement(),
                            popupId,
                            null,
                            date,
                            time);
            memberReservationRepository.save(reservation);
        }
    }

    private void assertPopupDate(AvailableDateResponse response) {
        Assertions.assertAll(
                () -> assertThat(response.popupOpenDate()).isEqualTo("2025-05-31"),
                () -> assertThat(response.popupCloseDate()).isEqualTo("2025-06-02"));
    }
}
