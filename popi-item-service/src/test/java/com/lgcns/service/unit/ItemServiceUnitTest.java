package com.lgcns.service.unit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

import com.lgcns.client.ManagerServiceClient;
import com.lgcns.dto.response.ItemInfoResponse;
import com.lgcns.response.SliceResponse;
import com.lgcns.service.ItemServiceImpl;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ItemServiceUnitTest {

    @InjectMocks private ItemServiceImpl itemService;

    @Mock private ManagerServiceClient managerServiceClient;

    @Nested
    class 상품_목록을_조회할_때 {

        @Test
        void 데이터가_존재하면_상품_목록_조회에_성공한다() {
            // given
            given(managerServiceClient.findItemsByName(anyLong(), isNull(), isNull(), anyInt()))
                    .willReturn(
                            new SliceResponse<>(
                                    List.of(
                                            new ItemInfoResponse(
                                                    24L,
                                                    "크룽크 빅쿠션",
                                                    "https://ygselect.com/web/product/big/shop1_c46529e55a48ad83a68b0f78540465e8.jpg",
                                                    25000),
                                            new ItemInfoResponse(
                                                    23L,
                                                    "크룽크 미니쿠션",
                                                    "https://ygselect.com/web/product/big/shop1_6e08c64b2daaf6f2142cbd32c9c7a38a.jpg",
                                                    18000),
                                            new ItemInfoResponse(
                                                    22L,
                                                    "크룽크 키링",
                                                    "https://ygselect.com/web/product/big/shop1_d2725a478a33ccaa10b1b7f8697f5c9d.png",
                                                    14000),
                                            new ItemInfoResponse(
                                                    21L,
                                                    "크룽크 손목인형",
                                                    "https://ygselect.com/web/product/big/202403/P0000HDL.jpg",
                                                    7000)),
                                    false));

            // when
            SliceResponse<ItemInfoResponse> result = itemService.findItemsByName(1L, null, null, 4);

            // then
            verify(managerServiceClient, times(1))
                    .findItemsByName(anyLong(), isNull(), isNull(), anyInt());
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.content()).hasSize(4),

                    // 각 항목의 필드 검증
                    () -> assertThat(result.content().get(0).itemId()).isEqualTo(24L),
                    () -> assertThat(result.content().get(1).itemId()).isEqualTo(23L),
                    () -> assertThat(result.content().get(2).itemId()).isEqualTo(22L),
                    () -> assertThat(result.content().get(3).itemId()).isEqualTo(21L),
                    () -> assertThat(result.isLast()).isFalse());
        }

        @Test
        void 마지막_상품까지_조회하면_is_last가_true를_반환한다() {
            // given
            given(managerServiceClient.findItemsByName(anyLong(), isNull(), anyLong(), anyInt()))
                    .willReturn(
                            new SliceResponse<>(
                                    List.of(
                                            new ItemInfoResponse(
                                                    4L,
                                                    "DAZED 리사",
                                                    "https://image.aladin.co.kr/product/17920/59/cover500/k142534034_1.jpg",
                                                    8000),
                                            new ItemInfoResponse(
                                                    3L,
                                                    "DAZED 제니",
                                                    "https://image.aladin.co.kr/product/28453/14/cover500/k572835617_1.jpg",
                                                    9500),
                                            new ItemInfoResponse(
                                                    2L,
                                                    "DAZED 로제",
                                                    "https://ygselect.com/web/product/big/202404/257d66b0a1eca57af67b6c792e68ed75.jpg",
                                                    15000),
                                            new ItemInfoResponse(
                                                    1L,
                                                    "DAZED 지수",
                                                    "https://ygselect.com/web/product/big/202402/P0000NMY.jpg",
                                                    15000)),
                                    true));

            // when
            SliceResponse<ItemInfoResponse> result = itemService.findItemsByName(1L, null, 5L, 4);

            // then
            verify(managerServiceClient, times(1))
                    .findItemsByName(anyLong(), isNull(), anyLong(), anyInt());
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.content()).hasSize(4),

                    // 각 항목의 필드 검증
                    () -> assertThat(result.content().get(0).itemId()).isEqualTo(4L),
                    () -> assertThat(result.content().get(1).itemId()).isEqualTo(3L),
                    () -> assertThat(result.content().get(2).itemId()).isEqualTo(2L),
                    () -> assertThat(result.content().get(3).itemId()).isEqualTo(1L),
                    () -> assertThat(result.isLast()).isTrue());
        }
    }

    @Nested
    class 상품_목록을_검색할_때 {

        @Test
        void 검색어에_대해_결과가_존재하면_상품_검색에_성공한다() {
            // given
            given(managerServiceClient.findItemsByName(anyLong(), anyString(), isNull(), anyInt()))
                    .willReturn(
                            new SliceResponse<>(
                                    List.of(
                                            new ItemInfoResponse(
                                                    4L,
                                                    "DAZED 리사",
                                                    "https://image.aladin.co.kr/product/17920/59/cover500/k142534034_1.jpg",
                                                    8000),
                                            new ItemInfoResponse(
                                                    3L,
                                                    "DAZED 제니",
                                                    "https://image.aladin.co.kr/product/28453/14/cover500/k572835617_1.jpg",
                                                    9500),
                                            new ItemInfoResponse(
                                                    2L,
                                                    "DAZED 로제",
                                                    "https://ygselect.com/web/product/big/202404/257d66b0a1eca57af67b6c792e68ed75.jpg",
                                                    15000),
                                            new ItemInfoResponse(
                                                    1L,
                                                    "DAZED 지수",
                                                    "https://ygselect.com/web/product/big/202402/P0000NMY.jpg",
                                                    15000)),
                                    true));

            // when
            SliceResponse<ItemInfoResponse> result =
                    itemService.findItemsByName(1L, "DAZED", null, 4);

            // then
            verify(managerServiceClient, times(1))
                    .findItemsByName(anyLong(), anyString(), isNull(), anyInt());
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.content()).hasSize(4),
                    () -> assertThat(result.isLast()).isTrue(),
                    () ->
                            assertThat(result.content())
                                    .allSatisfy(item -> assertThat(item.name()).contains("DAZED")));
        }

        @Test
        void 검색어에_대해_결과가_없으면_빈_리스트를_반환한다() {
            // given
            given(managerServiceClient.findItemsByName(anyLong(), anyString(), isNull(), anyInt()))
                    .willReturn(new SliceResponse<>(List.of(), true));

            // when
            SliceResponse<ItemInfoResponse> result =
                    itemService.findItemsByName(1L, "EMPTY", null, 4);

            // then
            verify(managerServiceClient, times(1))
                    .findItemsByName(anyLong(), anyString(), isNull(), anyInt());
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.content()).isEmpty(),
                    () -> assertThat(result.isLast()).isTrue());
        }
    }

    @Nested
    class 기본_상품_목록을_조회할_때 {
        @Test
        void 무작위로_선택된_4개의_상품_조회에_성공한다() {
            // given
            given(managerServiceClient.findItemsDefault(anyLong()))
                    .willReturn(
                            List.of(
                                    new ItemInfoResponse(
                                            18L,
                                            "크룽크 라운드백",
                                            "https://ygselect.com/web/product/big/shop1_cdf3bd187203a3987f4c262e73061334.png",
                                            20000),
                                    new ItemInfoResponse(
                                            4L,
                                            "DAZED 리사",
                                            "https://image.aladin.co.kr/product/17920/59/cover500/k142534034_1.jpg",
                                            8000),
                                    new ItemInfoResponse(
                                            17L,
                                            "크룽크 미니백",
                                            "https://ygselect.com/web/product/big/shop1_738be1088b092afab065be5299d5e2a4.png",
                                            15000),
                                    new ItemInfoResponse(
                                            9L,
                                            "피규어 지수",
                                            "https://m.ygselect.com/web/product/big/202110/51086d5fd28f00991986031b2ed3f742.jpg",
                                            84000)));

            // when
            List<ItemInfoResponse> result = itemService.findItemsDefault(1L);

            // then
            verify(managerServiceClient, times(1)).findItemsDefault(anyLong());
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).hasSize(4),
                    () -> assertThat(new HashSet<>(result)).hasSize(result.size()));
        }

        @Test
        void 상품_데이터가_4개보다_적으면_전체_데이터_수_만큼_상품이_조회된다() {
            // given
            given(managerServiceClient.findItemsDefault(anyLong()))
                    .willReturn(
                            List.of(
                                    new ItemInfoResponse(
                                            18L,
                                            "크룽크 라운드백",
                                            "https://ygselect.com/web/product/big/shop1_cdf3bd187203a3987f4c262e73061334.png",
                                            20000),
                                    new ItemInfoResponse(
                                            4L,
                                            "DAZED 리사",
                                            "https://image.aladin.co.kr/product/17920/59/cover500/k142534034_1.jpg",
                                            8000),
                                    new ItemInfoResponse(
                                            9L,
                                            "피규어 지수",
                                            "https://m.ygselect.com/web/product/big/202110/51086d5fd28f00991986031b2ed3f742.jpg",
                                            84000)));

            // when
            List<ItemInfoResponse> result = itemService.findItemsDefault(1L);

            // then
            verify(managerServiceClient, times(1)).findItemsDefault(anyLong());
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).hasSize(3),
                    () -> assertThat(new HashSet<>(result)).hasSize(result.size()));
        }
    }

    @Nested
    class 인기_상품_목록을_조회할_때 {
        @Test
        void 집계된_데이터가_3개_이상인_경우_인기_상품_전체_조회에_성공한다() {
            // given
            given(managerServiceClient.findItemsPopularity(anyLong()))
                    .willReturn(
                            List.of(
                                    new ItemInfoResponse(
                                            18L,
                                            "크룽크 라운드백",
                                            "https://ygselect.com/web/product/big/shop1_cdf3bd187203a3987f4c262e73061334.png",
                                            20000),
                                    new ItemInfoResponse(
                                            4L,
                                            "DAZED 리사",
                                            "https://image.aladin.co.kr/product/17920/59/cover500/k142534034_1.jpg",
                                            8000),
                                    new ItemInfoResponse(
                                            9L,
                                            "피규어 지수",
                                            "https://m.ygselect.com/web/product/big/202110/51086d5fd28f00991986031b2ed3f742.jpg",
                                            84000)));

            // when
            List<ItemInfoResponse> result = itemService.findItemsPopularity(1L);

            // then
            verify(managerServiceClient, times(1)).findItemsPopularity(anyLong());
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).hasSize(3),
                    () ->
                            assertThat(new HashSet<>(Objects.requireNonNull(result)))
                                    .hasSize(result.size()));
        }

        @Test
        void 집계된_데이터가_2개_경우_인기_상품_2개_조회에_성공한다() {
            // given
            given(managerServiceClient.findItemsPopularity(anyLong()))
                    .willReturn(
                            List.of(
                                    new ItemInfoResponse(
                                            18L,
                                            "크룽크 라운드백",
                                            "https://ygselect.com/web/product/big/shop1_cdf3bd187203a3987f4c262e73061334.png",
                                            20000),
                                    new ItemInfoResponse(
                                            4L,
                                            "DAZED 리사",
                                            "https://image.aladin.co.kr/product/17920/59/cover500/k142534034_1.jpg",
                                            8000)));

            // when
            List<ItemInfoResponse> result = itemService.findItemsPopularity(1L);

            // then
            verify(managerServiceClient, times(1)).findItemsPopularity(anyLong());
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).hasSize(2),
                    () ->
                            assertThat(new HashSet<>(Objects.requireNonNull(result)))
                                    .hasSize(result.size()));
        }

        @Test
        void 집계된_데이터가_1개인_경우_인기_상품_1개_조회에_성공한다() {
            // given
            given(managerServiceClient.findItemsPopularity(anyLong()))
                    .willReturn(
                            List.of(
                                    new ItemInfoResponse(
                                            18L,
                                            "크룽크 라운드백",
                                            "https://ygselect.com/web/product/big/shop1_cdf3bd187203a3987f4c262e73061334.png",
                                            20000)));

            // when
            List<ItemInfoResponse> result = itemService.findItemsPopularity(1L);

            // then
            verify(managerServiceClient, times(1)).findItemsPopularity(anyLong());
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(), () -> assertThat(result).hasSize(1));
        }

        @Test
        void 집계된_데이터가_없는_경우_빈_리스트를_반환한다() {
            // given
            given(managerServiceClient.findItemsPopularity(anyLong())).willReturn(List.of());

            // when
            List<ItemInfoResponse> result = itemService.findItemsPopularity(1L);

            // then
            verify(managerServiceClient, times(1)).findItemsPopularity(anyLong());
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(), () -> assertThat(result).isEmpty());
        }
    }
}
