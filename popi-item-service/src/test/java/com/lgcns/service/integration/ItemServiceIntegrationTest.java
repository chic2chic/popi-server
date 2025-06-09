package com.lgcns.service.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.lgcns.dto.response.ItemInfoResponse;
import com.lgcns.response.SliceResponse;
import com.lgcns.service.ItemService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

public class ItemServiceIntegrationTest extends WireMockIntegrationTest {

    @Autowired private ItemService itemService;
    @Autowired private ObjectMapper objectMapper;

    private final Long popupId = 1L;

    @Nested
    class 상품_목록을_조회할_때 {
        @Test
        void 데이터가_존재하면_상품_목록_조회에_성공한다() throws JsonProcessingException {
            // given
            int size = 4;

            String expectedResponse =
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

            stubFindItemsByName(popupId, null, null, 4, 200, expectedResponse);

            // when
            SliceResponse<ItemInfoResponse> result =
                    itemService.findItemsByName(popupId, null, null, size);

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
        void 마지막_상품까지_조회하면_is_last가_true를_반환한다() throws JsonProcessingException {
            // given
            Long lastItemId = 5L;
            int size = 4;

            String expectedResponse =
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

            stubFindItemsByName(popupId, null, 5L, 4, 200, expectedResponse);

            // when
            SliceResponse<ItemInfoResponse> result =
                    itemService.findItemsByName(popupId, null, lastItemId, size);

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

        @Test
        void 검색어에_대해_결과가_존재하면_상품_검색에_성공한다() throws JsonProcessingException {
            // given
            String keyword = "DAZED";
            int size = 4;

            String expectedResponse =
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

            stubFindItemsByName(popupId, keyword, null, 4, 200, expectedResponse);

            // when
            SliceResponse<ItemInfoResponse> result =
                    itemService.findItemsByName(popupId, keyword, null, size);

            // then
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.content()).hasSize(4),
                    () -> assertThat(result.isLast()).isTrue(),
                    () ->
                            assertThat(result.content())
                                    .allSatisfy(
                                            item ->
                                                    assertThat(item.name())
                                                            .contains("DAZED")) // 검색 결과 검증
                    );
        }

        @Test
        void 검색어에_대해_결과가_없으면_빈_리스트를_반환한다() throws JsonProcessingException {
            // given
            String keyword = "EMPTY";
            int size = 4;

            String expectedResponse =
                    objectMapper.writeValueAsString(Map.of("content", List.of(), "isLast", true));

            stubFindItemsByName(popupId, keyword, null, 4, 200, expectedResponse);

            // when
            SliceResponse<ItemInfoResponse> result =
                    itemService.findItemsByName(popupId, keyword, null, size);

            // then
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.content()).isEmpty(), // 빈 리스트 검증
                    () -> assertThat(result.isLast()).isTrue());
        }
    }

    @Nested
    class 기본_상품_목록을_조회할_때 {
        @Test
        void 무작위로_선택된_4개의_상품_조회에_성공한다() throws JsonProcessingException {
            // given
            String expectedResponse =
                    objectMapper.writeValueAsString(
                            List.of(
                                    Map.of(
                                            "itemId",
                                            18,
                                            "name",
                                            "크룽크 라운드백",
                                            "imageUrl",
                                            "https://ygselect.com/web/product/big/shop1_cdf3bd187203a3987f4c262e73061334.png",
                                            "price",
                                            20000),
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
                                            17,
                                            "name",
                                            "크룽크 미니백",
                                            "imageUrl",
                                            "https://ygselect.com/web/product/big/shop1_738be1088b092afab065be5299d5e2a4.png",
                                            "price",
                                            15000),
                                    Map.of(
                                            "itemId",
                                            9,
                                            "name",
                                            "피규어 지수",
                                            "imageUrl",
                                            "https://m.ygselect.com/web/product/big/202110/51086d5fd28f00991986031b2ed3f742.jpg",
                                            "price",
                                            84000)));

            stubFindItemsDefault(popupId, 200, expectedResponse);

            // when
            List<ItemInfoResponse> result = itemService.findItemsDefault(popupId);

            // then
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).hasSize(4),
                    () -> assertThat(new HashSet<>(result)).hasSize(result.size()));
        }

        @Test
        void 상품_데이터가_4개보다_적으면_전체_데이터_수_만큼_상품이_조회된다() throws JsonProcessingException {
            // given
            String expectedResponse =
                    objectMapper.writeValueAsString(
                            List.of(
                                    Map.of(
                                            "itemId",
                                            18,
                                            "name",
                                            "크룽크 라운드백",
                                            "imageUrl",
                                            "https://ygselect.com/web/product/big/shop1_cdf3bd187203a3987f4c262e73061334.png",
                                            "price",
                                            20000),
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
                                            9,
                                            "name",
                                            "피규어 지수",
                                            "imageUrl",
                                            "https://m.ygselect.com/web/product/big/202110/51086d5fd28f00991986031b2ed3f742.jpg",
                                            "price",
                                            84000)));

            stubFindItemsDefault(popupId, 200, expectedResponse);

            // when
            List<ItemInfoResponse> result = itemService.findItemsDefault(popupId);

            // then
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).hasSize(3),
                    () -> assertThat(new HashSet<>(result)).hasSize(result.size()));
        }
    }

    @Nested
    class 인기_상품_목록을_조회할_때 {
        @Test
        void 집계된_데이터가_3개_이상인_경우_인기_상품_전체_조회에_성공한다() throws JsonProcessingException {
            // given
            String expectedResponse =
                    objectMapper.writeValueAsString(
                            List.of(
                                    Map.of(
                                            "itemId",
                                            18,
                                            "name",
                                            "크룽크 라운드백",
                                            "imageUrl",
                                            "https://ygselect.com/web/product/big/shop1_cdf3bd187203a3987f4c262e73061334.png",
                                            "price",
                                            20000),
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
                                            9,
                                            "name",
                                            "피규어 지수",
                                            "imageUrl",
                                            "https://m.ygselect.com/web/product/big/202110/51086d5fd28f00991986031b2ed3f742.jpg",
                                            "price",
                                            84000)));

            stubFindItemsPopularity(popupId, 200, expectedResponse);

            // when
            List<ItemInfoResponse> result = itemService.findItemsPopularity(popupId);

            // then
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).hasSize(3),
                    () ->
                            assertThat(new HashSet<>(Objects.requireNonNull(result)))
                                    .hasSize(result.size()));
        }

        @Test
        void 집계된_데이터가_2개_경우_인기_상품_2개_조회에_성공한다() throws JsonProcessingException {
            // given
            String expectedResponse =
                    objectMapper.writeValueAsString(
                            List.of(
                                    Map.of(
                                            "itemId",
                                            18,
                                            "name",
                                            "크룽크 라운드백",
                                            "imageUrl",
                                            "https://ygselect.com/web/product/big/shop1_cdf3bd187203a3987f4c262e73061334.png",
                                            "price",
                                            20000),
                                    Map.of(
                                            "itemId",
                                            4,
                                            "name",
                                            "DAZED 리사",
                                            "imageUrl",
                                            "https://image.aladin.co.kr/product/17920/59/cover500/k142534034_1.jpg",
                                            "price",
                                            8000)));

            stubFindItemsPopularity(popupId, 200, expectedResponse);

            // when
            List<ItemInfoResponse> result = itemService.findItemsPopularity(popupId);

            // then
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).hasSize(2),
                    () ->
                            assertThat(new HashSet<>(Objects.requireNonNull(result)))
                                    .hasSize(result.size()));
        }

        @Test
        void 집계된_데이터가_1개인_경우_인기_상품_1개_조회에_성공한다() throws JsonProcessingException {
            // given
            String expectedResponse =
                    objectMapper.writeValueAsString(
                            List.of(
                                    Map.of(
                                            "itemId",
                                            18,
                                            "name",
                                            "크룽크 라운드백",
                                            "imageUrl",
                                            "https://ygselect.com/web/product/big/shop1_cdf3bd187203a3987f4c262e73061334.png",
                                            "price",
                                            20000)));

            stubFindItemsPopularity(popupId, 200, expectedResponse);

            // when
            List<ItemInfoResponse> result = itemService.findItemsPopularity(popupId);

            // then
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(), () -> assertThat(result).hasSize(1));
        }

        @Test
        void 집계된_데이터가_없는_경우_빈_리스트를_반환한다() throws JsonProcessingException {
            // given
            String expectedResponse = objectMapper.writeValueAsString(List.of());

            stubFindItemsPopularity(popupId, 200, expectedResponse);

            // when
            List<ItemInfoResponse> result = itemService.findItemsPopularity(popupId);

            // then
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(), () -> assertThat(result).isEmpty());
        }
    }

    private void stubFindItemsByName(
            Long popupId, String keyword, Long lastItemId, int size, int status, String body) {
        MappingBuilder mappingBuilder =
                get(urlPathEqualTo("/internal/popups/" + popupId + "/items"))
                        .withQueryParam("size", equalTo(String.valueOf(size)));

        mappingBuilder = applyKeywordIfPresent(mappingBuilder, keyword);
        mappingBuilder = applyLastItemIdIfPresent(mappingBuilder, lastItemId);

        wireMockServer.stubFor(
                mappingBuilder.willReturn(
                        aResponse()
                                .withStatus(status)
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(body)));
    }

    private void stubFindItemsDefault(Long popupId, int status, String body) {
        MappingBuilder mappingBuilder =
                get(urlPathEqualTo("/internal/popups/" + popupId + "/items/default"));

        wireMockServer.stubFor(
                mappingBuilder.willReturn(
                        aResponse()
                                .withStatus(status)
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(body)));
    }

    private void stubFindItemsPopularity(Long popupId, int status, String body) {
        MappingBuilder mappingBuilder =
                get(urlPathEqualTo("/internal/popups/" + popupId + "/items/popularity"));

        wireMockServer.stubFor(
                mappingBuilder.willReturn(
                        aResponse()
                                .withStatus(status)
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(body)));
    }

    private MappingBuilder applyKeywordIfPresent(MappingBuilder builder, String keyword) {
        return (keyword != null) ? builder.withQueryParam("keyword", equalTo(keyword)) : builder;
    }

    private MappingBuilder applyLastItemIdIfPresent(MappingBuilder builder, Long lastItemId) {
        return (lastItemId != null)
                ? builder.withQueryParam("lastItemId", equalTo(String.valueOf(lastItemId)))
                : builder;
    }
}
