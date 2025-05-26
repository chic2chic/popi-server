package com.lgcns.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.WireMockIntegrationTest;
import com.lgcns.dto.response.PopupDetailsResponse;
import com.lgcns.dto.response.PopupInfoResponse;
import com.lgcns.error.exception.CustomException;
import com.lgcns.response.SliceResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

public class PopupServiceTest extends WireMockIntegrationTest {

    @Autowired private PopupService popupService;
    @Autowired private ObjectMapper objectMapper;

    @Nested
    class 팝업_목록을_조회할_때 {

        @Test
        void 데이터가_존재하는_경우_리스트를_반환한다() throws JsonProcessingException {
            // given
            Long lastPopupId = 0L;
            int size = 8;

            String expectedResponse =
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "content",
                                    List.of(
                                            Map.of(
                                                    "popupId",
                                                    3,
                                                    "popupName",
                                                    "BLACKPINK 팝업스토어",
                                                    "imageUrl",
                                                    "https://bucket/blackpink.jpg",
                                                    "popupOpenDate",
                                                    "2025-05-01",
                                                    "popupCloseDate",
                                                    "2025-06-01",
                                                    "address",
                                                    "서울특별시 강남구 테헤란로 12, 1층 201호"),
                                            Map.of(
                                                    "popupId",
                                                    2,
                                                    "popupName",
                                                    "BTS 팝업스토어",
                                                    "imageUrl",
                                                    "https://bucket/bts.jpg",
                                                    "popupOpenDate",
                                                    "2025-05-15",
                                                    "popupCloseDate",
                                                    "2025-06-15",
                                                    "address",
                                                    "서울특별시 홍대입구역 2번 출구"),
                                            Map.of(
                                                    "popupId",
                                                    1,
                                                    "popupName",
                                                    "아이브 팝업스토어",
                                                    "imageUrl",
                                                    "https://bucket/ive.jpg",
                                                    "popupOpenDate",
                                                    "2025-06-01",
                                                    "popupCloseDate",
                                                    "2025-07-01",
                                                    "address",
                                                    "부산광역시 해운대구 마린시티")),
                                    "isLast",
                                    true));

            stubFindAllPopups(lastPopupId, size, 200, expectedResponse);

            // when
            SliceResponse<PopupInfoResponse> result =
                    popupService.findPopupsByName(null, lastPopupId, size);

            // then
            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(3);
            assertThat(result.isLast()).isTrue();
        }

        @Test
        void 운영중인_팝업이_없으면_빈_리스트를_반환한다() throws JsonProcessingException {
            // given
            Long lastPopupId = null;
            int size = 8;

            String expectedResponse =
                    objectMapper.writeValueAsString(Map.of("content", List.of(), "isLast", true));

            stubFindAllPopupsWithoutLastId(size, 200, expectedResponse);

            // when
            SliceResponse<PopupInfoResponse> result =
                    popupService.findPopupsByName(null, lastPopupId, size);

            // then
            assertThat(result).isNotNull();
            assertThat(result.content()).isEmpty();
            assertThat(result.isLast()).isTrue();
        }

        @Test
        void 정상적으로_페이징_처리에_성공한다() throws JsonProcessingException {
            // given - 첫 번째 페이지 설정
            Long firstLastPopupId = 0L;
            int size = 2;

            String expectedResponse =
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "content",
                                    List.of(
                                            Map.of(
                                                    "popupId",
                                                    5,
                                                    "popupName",
                                                    "레드벨벳 팝업스토어",
                                                    "imageUrl",
                                                    "https://bucket/redvelvet.jpg",
                                                    "popupOpenDate",
                                                    "2025-05-20",
                                                    "popupCloseDate",
                                                    "2025-06-20",
                                                    "address",
                                                    "대구광역시 중구 동성로"),
                                            Map.of(
                                                    "popupId",
                                                    4,
                                                    "popupName",
                                                    "에스파 팝업스토어",
                                                    "imageUrl",
                                                    "https://bucket/aespa.jpg",
                                                    "popupOpenDate",
                                                    "2025-06-10",
                                                    "popupCloseDate",
                                                    "2025-07-10",
                                                    "address",
                                                    "광주광역시 동구 충장로")),
                                    "isLast",
                                    false));

            stubFindAllPopups(firstLastPopupId, size, 200, expectedResponse);

            // when - 첫 번째 페이지 조회
            SliceResponse<PopupInfoResponse> firstResult =
                    popupService.findPopupsByName(null, firstLastPopupId, size);

            // then - 첫 번째 페이지 검증
            Assertions.assertAll(
                    () -> assertThat(firstResult).isNotNull(),
                    () -> assertThat(firstResult.content()).hasSize(2),
                    () -> assertThat(firstResult.content().get(0).popupId()).isEqualTo(5L),
                    () ->
                            assertThat(firstResult.content().get(0).popupName())
                                    .isEqualTo("레드벨벳 팝업스토어"),
                    () -> assertThat(firstResult.content().get(1).popupId()).isEqualTo(4L),
                    () ->
                            assertThat(firstResult.content().get(1).popupName())
                                    .isEqualTo("에스파 팝업스토어"),
                    () -> assertThat(firstResult.isLast()).isFalse());

            // given - 두 번째 페이지 설정 (WireMock 리셋 후 새로운 스텁 설정)
            wireMockServer.resetMappings();

            Long secondLastPopupId = 4L;
            expectedResponse =
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "content",
                                    List.of(
                                            Map.of(
                                                    "popupId",
                                                    3,
                                                    "popupName",
                                                    "뉴진스 팝업스토어",
                                                    "imageUrl",
                                                    "https://bucket/newjeans.jpg",
                                                    "popupOpenDate",
                                                    "2025-07-01",
                                                    "popupCloseDate",
                                                    "2025-08-01",
                                                    "address",
                                                    "인천광역시 연수구 송도동")),
                                    "isLast",
                                    true));

            stubFindAllPopups(secondLastPopupId, size, 200, expectedResponse);

            // when - 두 번째 페이지 조회
            SliceResponse<PopupInfoResponse> secondResult =
                    popupService.findPopupsByName(null, secondLastPopupId, size);

            // then - 두 번째 페이지 검증
            Assertions.assertAll(
                    () -> assertThat(secondResult).isNotNull(),
                    () -> assertThat(secondResult.content()).hasSize(1),
                    () -> assertThat(secondResult.content().get(0).popupId()).isEqualTo(3L),
                    () ->
                            assertThat(secondResult.content().get(0).popupName())
                                    .isEqualTo("뉴진스 팝업스토어"),
                    () -> assertThat(secondResult.isLast()).isTrue());
        }

        @Test
        void 검색어와_일치하는_데이터가_존재하면_결과_리스트를_반환한다() throws JsonProcessingException {
            // given
            String searchName = "BLACK";
            Long lastPopupId = null;
            int size = 8;

            String expectedResponse =
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "content",
                                    List.of(
                                            Map.of(
                                                    "popupId",
                                                    3,
                                                    "popupName",
                                                    "BLACKPINK 팝업스토어",
                                                    "imageUrl",
                                                    "https://bucket/blackpink.jpg",
                                                    "popupOpenDate",
                                                    "2025-05-01",
                                                    "popupCloseDate",
                                                    "2025-06-01",
                                                    "address",
                                                    "서울특별시 강남구 테헤란로 12, 1층 201호")),
                                    "isLast",
                                    true));

            stubFindPopupsByName(searchName, size, 200, expectedResponse);

            // when
            SliceResponse<PopupInfoResponse> result =
                    popupService.findPopupsByName(searchName, lastPopupId, size);

            // then
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.content()).hasSize(1),
                    () -> assertThat(result.content().get(0).popupId()).isEqualTo(3L),
                    () ->
                            assertThat(result.content().get(0).popupName())
                                    .isEqualTo("BLACKPINK 팝업스토어"),
                    () ->
                            assertThat(result.content().get(0).imageUrl())
                                    .isEqualTo("https://bucket/blackpink.jpg"),
                    () ->
                            assertThat(result.content().get(0).popupOpenDate())
                                    .isEqualTo("2025-05-01"),
                    () ->
                            assertThat(result.content().get(0).popupCloseDate())
                                    .isEqualTo("2025-06-01"),
                    () ->
                            assertThat(result.content().get(0).address())
                                    .isEqualTo("서울특별시 강남구 테헤란로 12, 1층 201호"),
                    () -> assertThat(result.isLast()).isTrue(),
                    () ->
                            assertThat(result.content())
                                    .allSatisfy(
                                            popup ->
                                                    assertThat(popup.popupName())
                                                            .contains(searchName)));
        }

        @Test
        void 검색어와_일치하는_데이터가_없으면_빈_리스트를_반환한다() throws JsonProcessingException {
            // given
            String searchName = "NONEXISTENT";
            Long lastPopupId = null;
            int size = 8;

            String expectedResponse =
                    objectMapper.writeValueAsString(Map.of("content", List.of(), "isLast", true));

            stubFindPopupsByName(searchName, size, 200, expectedResponse);

            // when
            SliceResponse<PopupInfoResponse> result =
                    popupService.findPopupsByName(searchName, lastPopupId, size);

            // then
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.content()).isEmpty(),
                    () -> assertThat(result.isLast()).isTrue());
        }
    }

    @Nested
    class 상품_상세_정보를_조회할_때 {

        @Test
        void 존재하는_팝업_아이디로_조회에_성공한다() throws JsonProcessingException {
            // given
            Long popupId = 1L;

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("popupId", 1);
            responseData.put("popupName", "BLACKPINK 팝업스토어");
            responseData.put("imageUrl", "https://bucket/blackpink.jpg");
            responseData.put("popupOpenDate", "2025-01-01");
            responseData.put("popupCloseDate", "2025-01-31");
            responseData.put("reservationOpenDateTime", "2025-01-01 10:00:00");
            responseData.put("reservationCloseDateTime", "2025-01-31 20:00:00");
            responseData.put("address", "서울특별시 강남구 테헤란로 123, 3층 A호");
            responseData.put("runOpenTime", "10:00:00");
            responseData.put("runCloseTime", "20:00:00");
            responseData.put("latitude", 37.123456);
            responseData.put("longitude", 127.123456);

            String expectedResponse = objectMapper.writeValueAsString(responseData);

            stubFindPopupDetailsById(popupId, 200, expectedResponse);

            // when
            PopupDetailsResponse result = popupService.findPopupDetailsById(popupId);

            // then
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.popupId()).isEqualTo(1L),
                    () -> assertThat(result.popupName()).isEqualTo("BLACKPINK 팝업스토어"),
                    () -> assertThat(result.imageUrl()).isEqualTo("https://bucket/blackpink.jpg"),
                    () -> assertThat(result.popupOpenDate()).isEqualTo("2025-01-01"),
                    () -> assertThat(result.popupCloseDate()).isEqualTo("2025-01-31"),
                    () ->
                            assertThat(result.reservationOpenDateTime())
                                    .isEqualTo("2025-01-01 10:00:00"),
                    () ->
                            assertThat(result.reservationCloseDateTime())
                                    .isEqualTo("2025-01-31 20:00:00"),
                    () -> assertThat(result.address()).isEqualTo("서울특별시 강남구 테헤란로 123, 3층 A호"),
                    () -> assertThat(result.runOpenTime()).isEqualTo("10:00:00"),
                    () -> assertThat(result.runCloseTime()).isEqualTo("20:00:00"),
                    () -> assertThat(result.latitude()).isEqualTo(37.123456),
                    () -> assertThat(result.longitude()).isEqualTo(127.123456));
        }

        @Test
        void 존재하지_않는_팝업_아이디로_조회하면_예외가_발생한다() throws JsonProcessingException {
            // given
            Long nonExistentPopupId = 9999L;

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
                                            "message", "팝업을 찾을 수 없습니다."),
                                    "timestamp",
                                    "2025-05-26T15:30:00"));

            stubFindPopupDetailsById(nonExistentPopupId, 404, expectedResponse);

            // when & then
            assertThatThrownBy(() -> popupService.findPopupDetailsById(nonExistentPopupId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode.errorName", "POPUP_NOT_FOUND")
                    .hasMessageContaining("팝업을 찾을 수 없습니다.");
        }
    }

    private void stubFindAllPopups(Long lastPopupId, int size, int status, String body) {
        wireMockServer.stubFor(
                get(urlPathEqualTo("/internal/popups"))
                        .withQueryParam("size", equalTo(String.valueOf(size)))
                        .withQueryParam("lastPopupId", equalTo(String.valueOf(lastPopupId)))
                        .willReturn(
                                aResponse()
                                        .withStatus(status)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(body)));
    }

    private void stubFindAllPopupsWithoutLastId(int size, int status, String body) {
        wireMockServer.stubFor(
                get(urlPathEqualTo("/internal/popups"))
                        .withQueryParam("size", equalTo(String.valueOf(size)))
                        .willReturn(
                                aResponse()
                                        .withStatus(status)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(body)));
    }

    private void stubFindPopupsByName(String searchName, int size, int status, String body) {
        wireMockServer.stubFor(
                get(urlPathEqualTo("/internal/popups"))
                        .withQueryParam("searchName", equalTo(searchName))
                        .withQueryParam("size", equalTo(String.valueOf(size)))
                        .willReturn(
                                aResponse()
                                        .withStatus(status)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(body)));
    }

    private void stubFindPopupDetailsById(Long popupId, int status, String body) {
        wireMockServer.stubFor(
                get(urlPathEqualTo("/internal/popups/" + popupId))
                        .willReturn(
                                aResponse()
                                        .withStatus(status)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(body)));
    }
}
