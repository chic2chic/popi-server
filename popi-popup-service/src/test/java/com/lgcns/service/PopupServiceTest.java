package com.lgcns.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.WireMockIntegrationTest;
import com.lgcns.dto.popup.response.PopupInfoResponse;
import com.lgcns.response.SliceResponse;
import com.lgcns.service.popup.PopupService;
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

            String responseBody =
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

            stubFindAllPopups(lastPopupId, size, 200, responseBody);

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

            String responseBody =
                    objectMapper.writeValueAsString(Map.of("content", List.of(), "isLast", true));

            stubFindAllPopupsWithoutLastId(size, 200, responseBody);

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

            String firstPageResponse =
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

            stubFindAllPopups(firstLastPopupId, size, 200, firstPageResponse);

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
            String secondPageResponse =
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

            stubFindAllPopups(secondLastPopupId, size, 200, secondPageResponse);

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
                        // lastPopupId 파라미터가 없는 경우
                        .willReturn(
                                aResponse()
                                        .withStatus(status)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(body)));
    }
}
