package com.lgcns.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.lgcns.WireMockIntegrationTest;
import com.lgcns.dto.popup.response.PopupInfoResponse;
import com.lgcns.response.SliceResponse;
import com.lgcns.service.popup.PopupService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

public class PopupServiceTest extends WireMockIntegrationTest {

    @Autowired private PopupService popupService;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        String responseBody1 =
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

        String responseBody2 =
                objectMapper.writeValueAsString(Map.of("content", List.of(), "isLast", true));

        String responseBody3 =
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

        String responseBody4 =
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

        stubFindAllPopups(0L, 8, 200, responseBody1);
        stubFindAllPopups(null, 8, 200, responseBody2);
        stubFindAllPopups(0L, 2, 200, responseBody3);
        stubFindAllPopups(4L, 2, 200, responseBody4);
    }

    @Nested
    class 팝업_목록_조회 {
        @Test
        void 팝업_목록_조회에_성공한다() {
            // given
            Long lastPopupId = 0L;
            int size = 8;

            // when
            SliceResponse<PopupInfoResponse> result = popupService.findAllPopups(lastPopupId, size);

            // then
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.content()).hasSize(3),
                    () -> assertThat(result.content().get(0).popupId()).isEqualTo(3L),
                    () ->
                            assertThat(result.content().get(0).popupName())
                                    .isEqualTo("BLACKPINK 팝업스토어"),
                    () -> assertThat(result.content().get(1).popupId()).isEqualTo(2L),
                    () -> assertThat(result.content().get(1).popupName()).isEqualTo("BTS 팝업스토어"),
                    () -> assertThat(result.content().get(2).popupId()).isEqualTo(1L),
                    () -> assertThat(result.content().get(2).popupName()).isEqualTo("아이브 팝업스토어"),
                    () -> assertThat(result.isLast()).isTrue());
        }

        @Test
        void 운영중인_팝업이_없으면_빈_리스트를_반환한다() {
            // given
            Long lastPopupId = null;
            int size = 8;

            // when
            SliceResponse<PopupInfoResponse> result = popupService.findAllPopups(lastPopupId, size);

            // then
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.content()).isEmpty(),
                    () -> assertThat(result.isLast()).isTrue());
        }

        @Test
        void 페이징_처리가_정상적으로_동작한다() {
            // given
            Long lastPopupId = 0L;
            int size = 2;

            // when
            SliceResponse<PopupInfoResponse> firstResult =
                    popupService.findAllPopups(lastPopupId, size);

            // then
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

            // when - 두 번째 페이지 조회
            SliceResponse<PopupInfoResponse> secondResult = popupService.findAllPopups(4L, size);

            // then
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
        MappingBuilder mappingBuilder =
                get(urlPathEqualTo("/internal/popups"))
                        .withQueryParam("size", equalTo(String.valueOf(size)));

        if (lastPopupId != null) {
            mappingBuilder =
                    mappingBuilder
                            .withQueryParam("lastPopupId", equalTo(String.valueOf(lastPopupId)))
                            .atPriority(1);
        } else {
            mappingBuilder = mappingBuilder.atPriority(2);
        }

        wireMockServer.stubFor(
                mappingBuilder.willReturn(
                        aResponse()
                                .withStatus(status)
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(body)));
    }
}
