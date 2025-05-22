package com.lgcns.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lgcns.IntegrationTest;
import com.lgcns.domain.MemberReservation;
import com.lgcns.dto.response.AvailableDateResponse;
import com.lgcns.dto.response.ReservableDate;
import com.lgcns.dto.response.ReservableTime;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.MemberReservationErrorCode;
import com.lgcns.repository.MemberReservationRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

@AutoConfigureWireMock(port = 8888)
class MemberReservationServiceTest extends IntegrationTest {

    @Autowired private MemberReservationService memberReservationService;

    @Autowired private MemberReservationRepository memberReservationRepository;

    private final String memberId = "1";
    private final Long popupId = 1L;
    private final AtomicLong memberIdGenerator = new AtomicLong(100);

    @BeforeEach
    void setUp() {
        // 6월 1일은 모두 찬 상태
        insertMemberReservation(LocalDate.of(2025, 6, 1), LocalTime.of(12, 0));
        insertMemberReservation(LocalDate.of(2025, 6, 1), LocalTime.of(13, 0));
        insertMemberReservation(LocalDate.of(2025, 6, 1), LocalTime.of(14, 0));

        // 6월 2일은 일부만 찬 상태
        insertMemberReservation(LocalDate.of(2025, 6, 2), LocalTime.of(13, 0));

        // OpenFeign Mocking
        stubFor(
                get(urlPathEqualTo("/reservations/popups/1"))
                        .withQueryParam("date", equalTo("2025-05"))
                        .willReturn(
                                okJson(
                                        """
                            {
                              "popupOpenDate": "2025-05-31",
                              "popupCloseDate": "2025-06-02",
                              "timeCapacity": 5,
                              "dailyReservations": [
                                {
                                  "reservationDate": "2025-05-31",
                                  "timeSlots": [
                                    {
                                      "reservationId": 1,
                                      "time": "12:00"
                                    },
                                    {
                                      "reservationId": 2,
                                      "time": "13:00"
                                    },
                                    {
                                      "reservationId": 3,
                                      "time": "14:00"
                                    }
                                  ]
                                }
                              ]
                            }
                        """)));

        stubFor(
                get(urlPathEqualTo("/reservations/popups/1"))
                        .withQueryParam("date", equalTo("2025-06"))
                        .willReturn(
                                okJson(
                                        """
                            {
                              "popupOpenDate": "2025-05-31",
                              "popupCloseDate": "2025-06-02",
                              "timeCapacity": 5,
                              "dailyReservations": [
                                {
                                  "reservationDate": "2025-06-01",
                                  "timeSlots": [
                                    {
                                      "reservationId": 4,
                                      "time": "12:00"
                                    },
                                    {
                                      "reservationId": 5,
                                      "time": "13:00"
                                    },
                                    {
                                      "reservationId": 6,
                                      "time": "14:00"
                                    }
                                  ]
                                },
                                {
                                  "reservationDate": "2025-06-02",
                                  "timeSlots": [
                                    {
                                      "reservationId": 7,
                                      "time": "12:00"
                                    },
                                    {
                                      "reservationId": 8,
                                      "time": "13:00"
                                    },
                                    {
                                      "reservationId": 9,
                                      "time": "14:00"
                                    }
                                  ]
                                }
                              ]
                            }
                        """)));

        stubFor(
                get(urlPathEqualTo("/reservations/popups/1"))
                        .withQueryParam("date", equalTo("2025-07"))
                        .willReturn(
                                okJson(
                                        """
                            {
                              "popupOpenDate": "2025-05-31",
                              "popupCloseDate": "2025-06-02",
                              "timeCapacity": 5,
                              "dailyReservations": []
                            }
                        """)));

        stubFor(
                get(urlPathEqualTo("/reservations/popups/999"))
                        .withQueryParam("date", equalTo("2025-06"))
                        .willReturn(
                                aResponse()
                                        .withStatus(404)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                """
                                    {
                                      "errorCode": "POPUP_NOT_FOUND",
                                      "message": "해당 팝업이 존재하지 않습니다."
                                    }
                                """)));
    }

    @DisplayName("날짜에 문자가 포함된 경우 예외가 발생한다.")
    @Test
    void 날짜에_문자가_포함되면_예외발생() {
        // given
        String date = "2025-June";

        // when & then
        assertThatThrownBy(
                        () -> memberReservationService.findAvailableDate(memberId, popupId, date))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode", MemberReservationErrorCode.INVALID_DATE_FORMAT);
    }

    @DisplayName("yyyy-MM 날짜 형식이 아닌 경우 예외가 발생한다.")
    @Test
    void 날짜_형식이_yyyy_MM이_아니면_예외발생() {
        // given
        String date = "2025-5";

        // when & then
        assertThatThrownBy(
                        () -> memberReservationService.findAvailableDate(memberId, popupId, date))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode", MemberReservationErrorCode.INVALID_DATE_FORMAT);
    }

    @DisplayName("1월부터 12월이 아닌 날짜가 들어오면 예외가 발생한다.")
    @Test
    void 월이_1부터_12가_아니면_예외발생() {
        // given
        String date = "2025-13";

        // when & then
        assertThatThrownBy(
                        () -> memberReservationService.findAvailableDate(memberId, popupId, date))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode", MemberReservationErrorCode.INVALID_DATE_FORMAT);
    }

    @DisplayName("예약이 전혀 없는 경우 모든 슬롯이 예약 가능해야 한다.")
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

    @DisplayName("모든 슬롯이 예약된 날짜는 예약 불가로 표시된다.")
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

    @DisplayName("일부 시간만 예약된 날짜는 예약 가능이며, 해당 시간은 예약 불가로 표시된다.")
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

    @DisplayName("팝업 예약 기간이 아닌 경우 가능한 날짜는 빈 리스트를 반환한다.")
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

    private void insertMemberReservation(LocalDate date, LocalTime time) {
        for (int i = 0; i < 5; i++) {
            MemberReservation reservation =
                    MemberReservation.createMemberReservation(
                            memberIdGenerator.getAndIncrement(), popupId, null, date, time);
            memberReservationRepository.save(reservation);
        }
    }

    private void assertPopupDate(AvailableDateResponse response) {
        Assertions.assertAll(
                () -> assertThat(response.popupOpenDate()).isEqualTo("2025-05-31"),
                () -> assertThat(response.popupCloseDate()).isEqualTo("2025-06-02"));
    }
}
