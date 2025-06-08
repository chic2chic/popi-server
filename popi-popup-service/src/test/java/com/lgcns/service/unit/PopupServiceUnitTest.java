package com.lgcns.service.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.lgcns.client.managerClient.ManagerServiceClient;
import com.lgcns.client.managerClient.dto.PopupIdsRequest;
import com.lgcns.client.reservationClient.ReservationServiceClient;
import com.lgcns.dto.response.PopupDetailsResponse;
import com.lgcns.dto.response.PopupInfoResponse;
import com.lgcns.dto.response.PopupMapResponse;
import com.lgcns.error.exception.CustomException;
import com.lgcns.error.feign.FeignErrorCode;
import com.lgcns.response.SliceResponse;
import com.lgcns.service.PopupServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PopupServiceUnitTest {

    @InjectMocks private PopupServiceImpl popupService;
    @Mock private ManagerServiceClient managerServiceClient;
    @Mock private ReservationServiceClient reservationServiceClient;

    @Nested
    class 팝업_목록을_조회할_때 {

        @Test
        void 데이터가_존재하는_경우_리스트를_반환한다() {
            // given
            Long lastPopupId = 0L;
            int size = 8;

            List<PopupInfoResponse> mockPopups =
                    List.of(
                            createPopupInfoResponse(3L, "BLACKPINK 팝업스토어"),
                            createPopupInfoResponse(2L, "BTS 팝업스토어"),
                            createPopupInfoResponse(1L, "아이브 팝업스토어"));
            SliceResponse<PopupInfoResponse> mockResponse = new SliceResponse<>(mockPopups, true);

            given(managerServiceClient.findPopupsByName(null, lastPopupId, size))
                    .willReturn(mockResponse);

            // when
            SliceResponse<PopupInfoResponse> result =
                    popupService.findPopupsByName(null, lastPopupId, size);

            // then
            verify(managerServiceClient, times(1)).findPopupsByName(null, lastPopupId, size);
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.content()).hasSize(3),
                    () -> assertThat(result.isLast()).isTrue());
        }

        @Test
        void 운영중인_팝업이_없으면_빈_리스트를_반환한다() {
            // given
            Long lastPopupId = null;
            int size = 8;

            SliceResponse<PopupInfoResponse> mockResponse = new SliceResponse<>(List.of(), true);

            given(managerServiceClient.findPopupsByName(null, lastPopupId, size))
                    .willReturn(mockResponse);

            // when
            SliceResponse<PopupInfoResponse> result =
                    popupService.findPopupsByName(null, lastPopupId, size);

            // then
            verify(managerServiceClient, times(1)).findPopupsByName(null, lastPopupId, size);
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.content()).isEmpty(),
                    () -> assertThat(result.isLast()).isTrue());
        }

        @Test
        void 정상적으로_페이징_처리에_성공한다() {
            // given
            Long firstLastPopupId = 0L;
            int size = 2;

            List<PopupInfoResponse> firstPagePopups =
                    List.of(
                            createPopupInfoResponse(5L, "레드벨벳 팝업스토어"),
                            createPopupInfoResponse(4L, "에스파 팝업스토어"));
            SliceResponse<PopupInfoResponse> firstPageResponse =
                    new SliceResponse<>(firstPagePopups, false);

            given(managerServiceClient.findPopupsByName(null, firstLastPopupId, size))
                    .willReturn(firstPageResponse);

            // when
            SliceResponse<PopupInfoResponse> firstResult =
                    popupService.findPopupsByName(null, firstLastPopupId, size);

            // then
            verify(managerServiceClient, times(1)).findPopupsByName(null, firstLastPopupId, size);
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

            // given
            Long secondLastPopupId = 4L;
            List<PopupInfoResponse> secondPagePopups =
                    List.of(createPopupInfoResponse(3L, "뉴진스 팝업스토어"));
            SliceResponse<PopupInfoResponse> secondPageResponse =
                    new SliceResponse<>(secondPagePopups, true);

            given(managerServiceClient.findPopupsByName(null, secondLastPopupId, size))
                    .willReturn(secondPageResponse);

            // when
            SliceResponse<PopupInfoResponse> secondResult =
                    popupService.findPopupsByName(null, secondLastPopupId, size);

            // then
            verify(managerServiceClient, times(1)).findPopupsByName(null, secondLastPopupId, size);
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
        void 검색어와_일치하는_데이터가_존재하면_결과_리스트를_반환한다() {
            // given
            String keyword = "BLACK";
            Long lastPopupId = null;
            int size = 8;

            List<PopupInfoResponse> mockPopups =
                    List.of(createPopupInfoResponse(3L, "BLACKPINK 팝업스토어"));
            SliceResponse<PopupInfoResponse> mockResponse = new SliceResponse<>(mockPopups, true);

            given(managerServiceClient.findPopupsByName(keyword, lastPopupId, size))
                    .willReturn(mockResponse);

            // when
            SliceResponse<PopupInfoResponse> result =
                    popupService.findPopupsByName(keyword, lastPopupId, size);

            // then
            verify(managerServiceClient, times(1)).findPopupsByName(keyword, lastPopupId, size);
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
                                                            .contains(keyword)));
        }

        @Test
        void 검색어와_일치하는_데이터가_없으면_빈_리스트를_반환한다() {
            // given
            String keyword = "NONEXISTENT";
            Long lastPopupId = null;
            int size = 8;

            SliceResponse<PopupInfoResponse> mockResponse = new SliceResponse<>(List.of(), true);

            given(managerServiceClient.findPopupsByName(keyword, lastPopupId, size))
                    .willReturn(mockResponse);

            // when
            SliceResponse<PopupInfoResponse> result =
                    popupService.findPopupsByName(keyword, lastPopupId, size);

            // then
            verify(managerServiceClient, times(1)).findPopupsByName(keyword, lastPopupId, size);
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.content()).isEmpty(),
                    () -> assertThat(result.isLast()).isTrue());
        }
    }

    @Nested
    class 상품_상세_정보를_조회할_때 {

        @Test
        void 존재하는_팝업_아이디로_조회에_성공한다() {
            // given
            Long popupId = 1L;

            PopupDetailsResponse mockResponse =
                    createPopupDetailsResponse(1L, "BLACKPINK 팝업스토어", 37.123456, 127.123456);

            given(managerServiceClient.findPopupDetailsById(popupId)).willReturn(mockResponse);

            // when
            PopupDetailsResponse result = popupService.findPopupDetailsById(popupId);

            // then
            verify(managerServiceClient, times(1)).findPopupDetailsById(popupId);
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
        void 존재하지_않는_팝업_아이디로_조회하면_예외가_발생한다() {
            // given
            Long nonExistentPopupId = 9999L;

            // FeignClient에서 404 응답을 받았을 때
            FeignErrorCode popupNotFoundError =
                    new FeignErrorCode("POPUP_NOT_FOUND", "팝업을 찾을 수 없습니다.", 404);
            given(managerServiceClient.findPopupDetailsById(nonExistentPopupId))
                    .willThrow(new CustomException(popupNotFoundError));

            // when & then
            assertThatThrownBy(() -> popupService.findPopupDetailsById(nonExistentPopupId))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("팝업을 찾을 수 없습니다.");

            verify(managerServiceClient, times(1)).findPopupDetailsById(nonExistentPopupId);
        }
    }

    @Nested
    class 인기_팝업_목록을_조회할_때 {

        @Test
        void 인기_팝업_아이디가_4개인_경우_정확히_4개를_인기순으로_반환한다() {
            // given
            List<Long> hotPopupIds = List.of(7L, 3L, 1L, 5L);

            List<PopupInfoResponse> mockPopups =
                    List.of(
                            createPopupInfoResponse(7L, "에스파 팝업스토어"),
                            createPopupInfoResponse(3L, "BLACKPINK 팝업스토어"),
                            createPopupInfoResponse(1L, "BTS 팝업스토어"),
                            createPopupInfoResponse(5L, "뉴진스 팝업스토어"));

            given(reservationServiceClient.findHotPopupIds()).willReturn(hotPopupIds);
            given(managerServiceClient.findHotPopupsByIds(any(PopupIdsRequest.class)))
                    .willReturn(mockPopups);

            // when
            List<PopupInfoResponse> result = popupService.findHotPopups();

            // then
            verify(reservationServiceClient, times(1)).findHotPopupIds();
            verify(managerServiceClient, times(1)).findHotPopupsByIds(any(PopupIdsRequest.class));
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).hasSize(4),
                    () -> assertThat(result.get(0).popupId()).isEqualTo(7L),
                    () -> assertThat(result.get(0).popupName()).isEqualTo("에스파 팝업스토어"),
                    () -> assertThat(result.get(1).popupId()).isEqualTo(3L),
                    () -> assertThat(result.get(1).popupName()).isEqualTo("BLACKPINK 팝업스토어"),
                    () -> assertThat(result.get(2).popupId()).isEqualTo(1L),
                    () -> assertThat(result.get(2).popupName()).isEqualTo("BTS 팝업스토어"),
                    () -> assertThat(result.get(3).popupId()).isEqualTo(5L),
                    () -> assertThat(result.get(3).popupName()).isEqualTo("뉴진스 팝업스토어"),
                    () -> {
                        List<Long> resultIds =
                                result.stream().map(PopupInfoResponse::popupId).toList();
                        assertThat(resultIds).containsExactly(7L, 3L, 1L, 5L);
                    });
        }

        @Test
        void 인기_팝업_아이디가_4개보다_많은_경우_인기순으로_4개만_반환한다() {
            // given
            List<Long> hotPopupIds = List.of(9L, 2L, 6L, 1L, 8L, 4L);

            List<PopupInfoResponse> mockPopups =
                    List.of(
                            createPopupInfoResponse(9L, "뉴진스 팝업스토어"),
                            createPopupInfoResponse(2L, "IVE 팝업스토어"),
                            createPopupInfoResponse(6L, "레드벨벳 팝업스토어"),
                            createPopupInfoResponse(1L, "BTS 팝업스토어"));

            given(reservationServiceClient.findHotPopupIds()).willReturn(hotPopupIds);
            given(managerServiceClient.findHotPopupsByIds(any(PopupIdsRequest.class)))
                    .willReturn(mockPopups);

            // when
            List<PopupInfoResponse> result = popupService.findHotPopups();

            // then
            verify(reservationServiceClient, times(1)).findHotPopupIds();
            verify(managerServiceClient, times(1)).findHotPopupsByIds(any(PopupIdsRequest.class));
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).hasSize(4),
                    () -> assertThat(result.get(0).popupId()).isEqualTo(9L),
                    () -> assertThat(result.get(1).popupId()).isEqualTo(2L),
                    () -> assertThat(result.get(2).popupId()).isEqualTo(6L),
                    () -> assertThat(result.get(3).popupId()).isEqualTo(1L),
                    () -> {
                        List<Long> resultIds =
                                result.stream().map(PopupInfoResponse::popupId).toList();
                        assertThat(resultIds).containsExactly(9L, 2L, 6L, 1L);
                    });
        }

        @Test
        void 인기_팝업_아이디가_4개_미만인_경우_인기순_우선하여_랜덤으로_채워서_4개를_반환한다() {
            // given
            List<Long> hotPopupIds = List.of(5L, 2L);

            List<PopupInfoResponse> mockPopups =
                    List.of(
                            createPopupInfoResponse(5L, "뉴진스 팝업스토어"),
                            createPopupInfoResponse(2L, "IVE 팝업스토어"),
                            createPopupInfoResponse(7L, "에스파 팝업스토어"),
                            createPopupInfoResponse(1L, "BTS 팝업스토어"));

            given(reservationServiceClient.findHotPopupIds()).willReturn(hotPopupIds);
            given(managerServiceClient.findHotPopupsByIds(any(PopupIdsRequest.class)))
                    .willReturn(mockPopups);

            // when
            List<PopupInfoResponse> result = popupService.findHotPopups();

            // then
            verify(reservationServiceClient, times(1)).findHotPopupIds();
            verify(managerServiceClient, times(1)).findHotPopupsByIds(any(PopupIdsRequest.class));
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).hasSize(4),
                    () -> assertThat(result.get(0).popupId()).isEqualTo(5L),
                    () -> assertThat(result.get(1).popupId()).isEqualTo(2L),
                    () -> assertThat(result.get(2).popupId()).isEqualTo(7L),
                    () -> assertThat(result.get(3).popupId()).isEqualTo(1L),
                    () -> {
                        List<Long> resultIds =
                                result.stream().map(PopupInfoResponse::popupId).toList();
                        assertThat(resultIds).contains(5L, 2L);
                    });
        }

        @Test
        void 인기_팝업_아이디가_빈_리스트면_랜덤으로_4개를_반환한다() {
            // given
            List<Long> emptyPopupIds = List.of();

            List<PopupInfoResponse> mockPopups =
                    List.of(
                            createPopupInfoResponse(3L, "BLACKPINK 팝업스토어"),
                            createPopupInfoResponse(8L, "레드벨벳 팝업스토어"),
                            createPopupInfoResponse(1L, "BTS 팝업스토어"),
                            createPopupInfoResponse(6L, "뉴진스 팝업스토어"));

            given(reservationServiceClient.findHotPopupIds()).willReturn(emptyPopupIds);
            given(managerServiceClient.findHotPopupsByIds(any(PopupIdsRequest.class)))
                    .willReturn(mockPopups);

            // when
            List<PopupInfoResponse> result = popupService.findHotPopups();

            // then
            verify(reservationServiceClient, times(1)).findHotPopupIds();
            verify(managerServiceClient, times(1)).findHotPopupsByIds(any(PopupIdsRequest.class));
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).hasSize(4),
                    () -> {
                        List<Long> resultIds =
                                result.stream().map(PopupInfoResponse::popupId).toList();
                        assertThat(resultIds).containsExactlyInAnyOrder(3L, 8L, 1L, 6L);
                    });
        }

        @Test
        void 전체_팝업이_4개_미만인_경우_존재하는_만큼만_반환한다() {
            // given
            List<Long> hotPopupIds = List.of(2L);

            List<PopupInfoResponse> mockPopups =
                    List.of(
                            createPopupInfoResponse(2L, "IVE 팝업스토어"),
                            createPopupInfoResponse(5L, "뉴진스 팝업스토어"),
                            createPopupInfoResponse(1L, "BTS 팝업스토어"));

            given(reservationServiceClient.findHotPopupIds()).willReturn(hotPopupIds);
            given(managerServiceClient.findHotPopupsByIds(any(PopupIdsRequest.class)))
                    .willReturn(mockPopups);

            // when
            List<PopupInfoResponse> result = popupService.findHotPopups();

            // then
            verify(reservationServiceClient, times(1)).findHotPopupIds();
            verify(managerServiceClient, times(1)).findHotPopupsByIds(any(PopupIdsRequest.class));
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).hasSize(3),
                    () -> assertThat(result.get(0).popupId()).isEqualTo(2L),
                    () -> assertThat(result.get(0).popupName()).isEqualTo("IVE 팝업스토어"),
                    () -> assertThat(result.get(1).popupId()).isEqualTo(5L),
                    () -> assertThat(result.get(2).popupId()).isEqualTo(1L),
                    () -> {
                        List<Long> resultIds =
                                result.stream().map(PopupInfoResponse::popupId).toList();
                        assertThat(resultIds).contains(2L);
                    });
        }
    }

    @Nested
    class 지도_기반_팝업_목록을_조회할_때 {

        @Test
        void 지정된_영역_내에_팝업이_존재하면_해당_팝업들을_반환한다() {
            // given
            // 서울
            Double latMin = 37.378638;
            Double latMax = 37.671877;
            Double lngMin = 126.799543;
            Double lngMax = 127.184881;

            List<PopupMapResponse> mockPopups =
                    List.of(
                            createPopupMapResponse(1L, "강남 BLACKPINK 팝업스토어", 37.411222, 126.999999),
                            createPopupMapResponse(2L, "홍대 BTS 팝업스토어", 37.511222, 127.110011));

            given(managerServiceClient.findPopupsByMapArea(latMin, latMax, lngMin, lngMax))
                    .willReturn(mockPopups);

            // when
            List<PopupMapResponse> result =
                    popupService.findPopupsByMapArea(latMin, latMax, lngMin, lngMax);

            // then
            verify(managerServiceClient, times(1))
                    .findPopupsByMapArea(latMin, latMax, lngMin, lngMax);
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).hasSize(2),
                    () -> assertThat(result.get(0).popupId()).isEqualTo(1L),
                    () -> assertThat(result.get(1).popupId()).isEqualTo(2L));
        }

        @Test
        void 지정된_영역_내에_팝업이_없으면_빈_리스트를_반환한다() {
            // given
            Double latMin = 37.378638;
            Double latMax = 37.671877;
            Double lngMin = 126.799543;
            Double lngMax = 127.184881;

            given(managerServiceClient.findPopupsByMapArea(latMin, latMax, lngMin, lngMax))
                    .willReturn(List.of());

            // when
            List<PopupMapResponse> result =
                    popupService.findPopupsByMapArea(latMin, latMax, lngMin, lngMax);

            // then
            verify(managerServiceClient, times(1))
                    .findPopupsByMapArea(latMin, latMax, lngMin, lngMax);
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(), () -> assertThat(result).isEmpty());
        }
    }

    private PopupInfoResponse createPopupInfoResponse(Long popupId, String popupName) {
        String imageUrl =
                "https://bucket/"
                        + popupName.toLowerCase().replace(" ", "").replace("팝업스토어", "")
                        + ".jpg";
        return PopupInfoResponse.of(
                popupId,
                popupName,
                imageUrl,
                "2025-05-01",
                "2025-06-01",
                "서울특별시 강남구 테헤란로 12, 1층 201호");
    }

    private PopupDetailsResponse createPopupDetailsResponse(
            Long popupId, String popupName, Double latitude, Double longitude) {
        String imageUrl =
                "https://bucket/"
                        + popupName.toLowerCase().replace(" ", "").replace("팝업스토어", "")
                        + ".jpg";
        return PopupDetailsResponse.of(
                popupId,
                popupName,
                imageUrl,
                "2025-01-01",
                "2025-01-31",
                "2025-01-01 10:00:00",
                "2025-01-31 20:00:00",
                "서울특별시 강남구 테헤란로 123, 3층 A호",
                "10:00:00",
                "20:00:00",
                latitude,
                longitude);
    }

    private PopupMapResponse createPopupMapResponse(
            Long popupId, String popupName, Double latitude, Double longitude) {
        return PopupMapResponse.of(
                popupId,
                popupName,
                "https://bucket/image.jpg",
                "2025-05-01",
                "2025-06-01",
                "서울특별시 강남구 테헤란로 123",
                latitude,
                longitude);
    }
}
