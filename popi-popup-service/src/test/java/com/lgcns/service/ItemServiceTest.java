package com.lgcns.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.lgcns.WireMockIntegrationTest;
import com.lgcns.dto.item.response.ItemInfoResponse;
import com.lgcns.response.SliceResponse;
import com.lgcns.service.item.ItemService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

public class ItemServiceTest extends WireMockIntegrationTest {

    @Autowired private ItemService itemService;
    @Autowired private ObjectMapper objectMapper;

    private final Long popupId = 1L;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        String responseBody1 =
                objectMapper.writeValueAsString(
                        Map.of(
                                "content",
                                List.of(
                                        Map.of(
                                                "itemId",
                                                24,
                                                "name",
                                                "크룽크 빅쿠션",
                                                "imageUrl",
                                                "https://ygselect.com/web/product/big/shop1_c46529e55a48ad83a68b0f78540465e8.jpg",
                                                "price",
                                                25000),
                                        Map.of(
                                                "itemId",
                                                23,
                                                "name",
                                                "크룽크 미니쿠션",
                                                "imageUrl",
                                                "https://ygselect.com/web/product/big/shop1_6e08c64b2daaf6f2142cbd32c9c7a38a.jpg",
                                                "price",
                                                18000),
                                        Map.of(
                                                "itemId",
                                                22,
                                                "name",
                                                "크룽크 키링",
                                                "imageUrl",
                                                "https://ygselect.com/web/product/big/shop1_d2725a478a33ccaa10b1b7f8697f5c9d.png",
                                                "price",
                                                14000),
                                        Map.of(
                                                "itemId",
                                                21,
                                                "name",
                                                "크룽크 손목인형",
                                                "imageUrl",
                                                "https://ygselect.com/web/product/big/202403/P0000HDL.jpg",
                                                "price",
                                                7000)),
                                "isLast",
                                false));

        String responseBody2 =
                objectMapper.writeValueAsString(
                        Map.of(
                                "content",
                                List.of(
                                        Map.of(
                                                "itemId",
                                                4,
                                                "name",
                                                "DAZED 리사",
                                                "imageUrl",
                                                "https://image.aladin.co.kr/product/17920/59/cover500/k142534034_1.jpg",
                                                "price",
                                                8000),
                                        Map.of(
                                                "itemId",
                                                3,
                                                "name",
                                                "DAZED 제니",
                                                "imageUrl",
                                                "https://image.aladin.co.kr/product/28453/14/cover500/k572835617_1.jpg",
                                                "price",
                                                9500),
                                        Map.of(
                                                "itemId",
                                                2,
                                                "name",
                                                "DAZED 로제",
                                                "imageUrl",
                                                "https://ygselect.com/web/product/big/202404/257d66b0a1eca57af67b6c792e68ed75.jpg",
                                                "price",
                                                15000),
                                        Map.of(
                                                "itemId",
                                                1,
                                                "name",
                                                "DAZED 지수",
                                                "imageUrl",
                                                "https://ygselect.com/web/product/big/202402/P0000NMY.jpg",
                                                "price",
                                                15000)),
                                "isLast",
                                true));
        stubFindAllItems(popupId, null, 4, 200, responseBody1);
        stubFindAllItems(popupId, 5L, 4, 200, responseBody2);
    }

    @Nested
    class 상품_목록_조회 {
        @Test
        void 상품_목록_조회에_성공한다() {
            // given
            int size = 4;

            // when
            SliceResponse<ItemInfoResponse> result = itemService.findAllItems(popupId, null, size);

            // then
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.content()).hasSize(4),

                    // 각 항목의 필드 검증
                    () -> assertThat(result.content().get(0).itemId()).isEqualTo(24L),
                    () -> assertThat(result.content().get(1).itemId()).isEqualTo(23L),
                    () -> assertThat(result.content().get(2).itemId()).isEqualTo(22L),
                    () -> assertThat(result.content().get(3).itemId()).isEqualTo(21L),
                    () -> assertThat(result.isLast()).isFalse() // isLast=false 검증
                    );
        }

        @Test
        void 마지막_상품까지_조회하면_is_last가_true를_반환한다() {
            // given
            Long lastItemId = 5L;
            int size = 4;

            // when
            SliceResponse<ItemInfoResponse> result =
                    itemService.findAllItems(popupId, lastItemId, size);

            // then
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.content()).hasSize(4),

                    // 각 항목의 필드 검증
                    () -> assertThat(result.content().get(0).itemId()).isEqualTo(4L),
                    () -> assertThat(result.content().get(1).itemId()).isEqualTo(3L),
                    () -> assertThat(result.content().get(2).itemId()).isEqualTo(2L),
                    () -> assertThat(result.content().get(3).itemId()).isEqualTo(1L),
                    () -> assertThat(result.isLast()).isTrue() // isLast=true 검증
                    );
        }
    }

    private void stubFindAllItems(
            Long popupId, Long lastItemId, int size, int status, String body) {
        MappingBuilder mappingBuilder =
                get(urlPathEqualTo("/internal/popups/" + popupId + "/items"))
                        .withQueryParam("size", equalTo(String.valueOf(size)));

        if (lastItemId != null) {
            mappingBuilder =
                    mappingBuilder.withQueryParam(
                            "lastItemId", equalTo(String.valueOf(lastItemId)));
        }

        wireMockServer.stubFor(
                mappingBuilder.willReturn(
                        aResponse()
                                .withStatus(status)
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(body)));
    }
}
