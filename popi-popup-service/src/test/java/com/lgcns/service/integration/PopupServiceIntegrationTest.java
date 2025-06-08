package com.lgcns.service.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.WireMockIntegrationTest;
import com.lgcns.client.managerClient.dto.PopupIdsRequest;
import com.lgcns.dto.response.PopupDetailsResponse;
import com.lgcns.dto.response.PopupInfoResponse;
import com.lgcns.dto.response.PopupMapResponse;
import com.lgcns.error.exception.CustomException;
import com.lgcns.response.SliceResponse;
import com.lgcns.service.PopupService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

public class PopupServiceIntegrationTest extends WireMockIntegrationTest {

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
            String keyword = "BLACK";
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

            stubFindPopupsByName(keyword, size, 200, expectedResponse);

            // when
            SliceResponse<PopupInfoResponse> result =
                    popupService.findPopupsByName(keyword, lastPopupId, size);

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
                                                            .contains(keyword)));
        }

        @Test
        void 검색어와_일치하는_데이터가_없으면_빈_리스트를_반환한다() throws JsonProcessingException {
            // given
            String keyword = "NONEXISTENT";
            Long lastPopupId = null;
            int size = 8;

            String expectedResponse =
                    objectMapper.writeValueAsString(Map.of("content", List.of(), "isLast", true));

            stubFindPopupsByName(keyword, size, 200, expectedResponse);

            // when
            SliceResponse<PopupInfoResponse> result =
                    popupService.findPopupsByName(keyword, lastPopupId, size);

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

    @Nested
    class 인기_팝업_목록을_조회할_때 {

        @Test
        void 인기_팝업_아이디가_4개인_경우_정확히_4개를_인기순으로_반환한다() throws JsonProcessingException {
            // given
            List<Long> hotPopupIds = List.of(7L, 3L, 1L, 5L); // 인기순
            PopupIdsRequest hotPopupIdsRequest = new PopupIdsRequest(hotPopupIds);

            String expectedIdsResponse = objectMapper.writeValueAsString(hotPopupIds);

            String expectedResponse =
                    objectMapper.writeValueAsString(
                            List.of(
                                    Map.of(
                                            "popupId", 7,
                                            "popupName", "에스파 팝업스토어",
                                            "imageUrl", "https://bucket/aespa.jpg",
                                            "popupOpenDate", "2025-05-20",
                                            "popupCloseDate", "2025-06-20",
                                            "address", "서울특별시 강남구 테헤란로 7, 4층"),
                                    Map.of(
                                            "popupId", 3,
                                            "popupName", "BLACKPINK 팝업스토어",
                                            "imageUrl", "https://bucket/blackpink.jpg",
                                            "popupOpenDate", "2025-05-10",
                                            "popupCloseDate", "2025-06-10",
                                            "address", "서울특별시 강남구 테헤란로 3, 2층"),
                                    Map.of(
                                            "popupId", 1,
                                            "popupName", "BTS 팝업스토어",
                                            "imageUrl", "https://bucket/bts.jpg",
                                            "popupOpenDate", "2025-05-01",
                                            "popupCloseDate", "2025-06-01",
                                            "address", "서울특별시 강남구 테헤란로 1, 1층"),
                                    Map.of(
                                            "popupId", 5,
                                            "popupName", "뉴진스 팝업스토어",
                                            "imageUrl", "https://bucket/newjeans.jpg",
                                            "popupOpenDate", "2025-05-15",
                                            "popupCloseDate", "2025-06-15",
                                            "address", "서울특별시 강남구 테헤란로 5, 3층")));

            stubFindHotPopupIds(200, expectedIdsResponse);
            stubFindHotPopupsByIds(hotPopupIdsRequest, 200, expectedResponse);

            // when
            List<PopupInfoResponse> result = popupService.findHotPopups();

            // then
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
                        assertThat(resultIds).containsExactly(7L, 3L, 1L, 5L); // 정확한 순서 검증
                    });
        }

        @Test
        void 인기_팝업_아이디가_4개보다_많은_경우_인기순으로_4개만_반환한다() throws JsonProcessingException {
            // given
            List<Long> hotPopupIds = List.of(9L, 2L, 6L, 1L, 8L, 4L); // 인기순
            PopupIdsRequest hotPopupIdsRequest = new PopupIdsRequest(hotPopupIds);

            String expectedIdsResponse = objectMapper.writeValueAsString(hotPopupIds);

            String expectedResponse =
                    objectMapper.writeValueAsString(
                            List.of(
                                    Map.of(
                                            "popupId", 9,
                                            "popupName", "뉴진스 팝업스토어",
                                            "imageUrl", "https://bucket/newjeans.jpg",
                                            "popupOpenDate", "2025-05-01",
                                            "popupCloseDate", "2025-06-01",
                                            "address", "서울특별시 강남구 테헤란로 9, 1층"),
                                    Map.of(
                                            "popupId", 2,
                                            "popupName", "IVE 팝업스토어",
                                            "imageUrl", "https://bucket/ive.jpg",
                                            "popupOpenDate", "2025-05-05",
                                            "popupCloseDate", "2025-06-05",
                                            "address", "서울특별시 홍대입구역 2번 출구"),
                                    Map.of(
                                            "popupId", 6,
                                            "popupName", "레드벨벳 팝업스토어",
                                            "imageUrl", "https://bucket/redvelvet.jpg",
                                            "popupOpenDate", "2025-05-10",
                                            "popupCloseDate", "2025-06-10",
                                            "address", "서울특별시 강남구 테헤란로 6, 2층"),
                                    Map.of(
                                            "popupId", 1,
                                            "popupName", "BTS 팝업스토어",
                                            "imageUrl", "https://bucket/bts.jpg",
                                            "popupOpenDate", "2025-05-25",
                                            "popupCloseDate", "2025-06-25",
                                            "address", "부산광역시 해운대구 마린시티")));

            stubFindHotPopupIds(200, expectedIdsResponse);
            stubFindHotPopupsByIds(hotPopupIdsRequest, 200, expectedResponse);

            // when
            List<PopupInfoResponse> result = popupService.findHotPopups();

            // then
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
        void 인기_팝업_아이디가_4개_미만인_경우_인기순_우선하여_랜덤으로_채워서_4개를_반환한다() throws JsonProcessingException {
            // given
            List<Long> hotPopupIds = List.of(5L, 2L); // 인기순 2개
            PopupIdsRequest hotPopupIdsRequest = new PopupIdsRequest(hotPopupIds);

            String expectedIdsResponse = objectMapper.writeValueAsString(hotPopupIds);

            String expectedResponse =
                    objectMapper.writeValueAsString(
                            List.of(
                                    Map.of(
                                            "popupId", 5,
                                            "popupName", "뉴진스 팝업스토어",
                                            "imageUrl", "https://bucket/newjeans.jpg",
                                            "popupOpenDate", "2025-05-15",
                                            "popupCloseDate", "2025-06-15",
                                            "address", "서울특별시 강남구 테헤란로 5, 3층"),
                                    Map.of(
                                            "popupId", 2,
                                            "popupName", "IVE 팝업스토어",
                                            "imageUrl", "https://bucket/ive.jpg",
                                            "popupOpenDate", "2025-05-05",
                                            "popupCloseDate", "2025-06-05",
                                            "address", "서울특별시 홍대입구역 2번 출구"),
                                    Map.of(
                                            "popupId", 7,
                                            "popupName", "에스파 팝업스토어",
                                            "imageUrl", "https://bucket/aespa.jpg",
                                            "popupOpenDate", "2025-05-01",
                                            "popupCloseDate", "2025-06-01",
                                            "address", "서울특별시 강남구 테헤란로 1, 1층"),
                                    Map.of(
                                            "popupId", 1,
                                            "popupName", "BTS 팝업스토어",
                                            "imageUrl", "https://bucket/bts.jpg",
                                            "popupOpenDate", "2025-05-20",
                                            "popupCloseDate", "2025-06-20",
                                            "address", "부산광역시 해운대구 마린시티")));

            stubFindHotPopupIds(200, expectedIdsResponse);
            stubFindHotPopupsByIds(hotPopupIdsRequest, 200, expectedResponse);

            // when
            List<PopupInfoResponse> result = popupService.findHotPopups();

            // then
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).hasSize(4),
                    () -> assertThat(result.get(0).popupId()).isEqualTo(5L),
                    () -> assertThat(result.get(1).popupId()).isEqualTo(2L),
                    // 랜덤 팝업
                    () -> assertThat(result.get(2).popupId()).isEqualTo(7L),
                    () -> assertThat(result.get(3).popupId()).isEqualTo(1L),
                    () -> {
                        List<Long> resultIds =
                                result.stream().map(PopupInfoResponse::popupId).toList();
                        assertThat(resultIds).contains(5L, 2L);
                    });
        }

        @Test
        void 인기_팝업_아이디가_빈_리스트면_랜덤으로_4개를_반환한다() throws JsonProcessingException {
            // given
            List<Long> emptyPopupIds = List.of();
            PopupIdsRequest emptyPopupIdsRequest = new PopupIdsRequest(emptyPopupIds);

            String expectedIdsResponse = objectMapper.writeValueAsString(emptyPopupIds);

            String expectedResponse =
                    objectMapper.writeValueAsString(
                            List.of(
                                    Map.of(
                                            "popupId", 3,
                                            "popupName", "BLACKPINK 팝업스토어",
                                            "imageUrl", "https://bucket/blackpink.jpg",
                                            "popupOpenDate", "2025-05-10",
                                            "popupCloseDate", "2025-06-10",
                                            "address", "서울특별시 강남구 테헤란로 3, 2층"),
                                    Map.of(
                                            "popupId", 8,
                                            "popupName", "레드벨벳 팝업스토어",
                                            "imageUrl", "https://bucket/redvelvet.jpg",
                                            "popupOpenDate", "2025-05-25",
                                            "popupCloseDate", "2025-06-25",
                                            "address", "부산광역시 해운대구 마린시티"),
                                    Map.of(
                                            "popupId", 1,
                                            "popupName", "BTS 팝업스토어",
                                            "imageUrl", "https://bucket/bts.jpg",
                                            "popupOpenDate", "2025-05-01",
                                            "popupCloseDate", "2025-06-01",
                                            "address", "서울특별시 강남구 테헤란로 1, 1층"),
                                    Map.of(
                                            "popupId", 6,
                                            "popupName", "뉴진스 팝업스토어",
                                            "imageUrl", "https://bucket/newjeans.jpg",
                                            "popupOpenDate", "2025-05-15",
                                            "popupCloseDate", "2025-06-15",
                                            "address", "서울특별시 강남구 테헤란로 5, 3층")));

            stubFindHotPopupIds(200, expectedIdsResponse);
            stubFindHotPopupsByIds(emptyPopupIdsRequest, 200, expectedResponse);

            // when
            List<PopupInfoResponse> result = popupService.findHotPopups();

            // then
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
        void 전체_팝업이_4개_미만인_경우_존재하는_만큼만_반환한다() throws JsonProcessingException {
            // given
            List<Long> hotPopupIds = List.of(2L);
            PopupIdsRequest hotPopupIdsRequest = new PopupIdsRequest(hotPopupIds);

            String expectedIdsResponse = objectMapper.writeValueAsString(hotPopupIds);

            String expectedResponse =
                    objectMapper.writeValueAsString(
                            List.of(
                                    Map.of(
                                            "popupId", 2,
                                            "popupName", "IVE 팝업스토어",
                                            "imageUrl", "https://bucket/ive.jpg",
                                            "popupOpenDate", "2025-05-05",
                                            "popupCloseDate", "2025-06-05",
                                            "address", "서울특별시 홍대입구역 2번 출구"),
                                    Map.of(
                                            "popupId", 5,
                                            "popupName", "뉴진스 팝업스토어",
                                            "imageUrl", "https://bucket/newjeans.jpg",
                                            "popupOpenDate", "2025-05-15",
                                            "popupCloseDate", "2025-06-15",
                                            "address", "서울특별시 강남구 테헤란로 5, 3층"),
                                    Map.of(
                                            "popupId", 1,
                                            "popupName", "BTS 팝업스토어",
                                            "imageUrl", "https://bucket/bts.jpg",
                                            "popupOpenDate", "2025-05-01",
                                            "popupCloseDate", "2025-06-01",
                                            "address", "서울특별시 강남구 테헤란로 1, 1층")));

            stubFindHotPopupIds(200, expectedIdsResponse);
            stubFindHotPopupsByIds(hotPopupIdsRequest, 200, expectedResponse);

            // when
            List<PopupInfoResponse> result = popupService.findHotPopups();

            // then
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
        void 지정된_영역_내에_팝업이_존재하면_해당_팝업들을_반환한다() throws JsonProcessingException {
            // given
            // 서울
            Double latMin = 37.378638;
            Double latMax = 37.671877;
            Double lngMin = 126.799543;
            Double lngMax = 127.184881;

            String expectedResponse =
                    objectMapper.writeValueAsString(
                            List.of(
                                    Map.of(
                                            "popupId", 1,
                                            "popupName", "강남 BLACKPINK 팝업스토어",
                                            "imageUrl", "https://bucket/blackpink.jpg",
                                            "popupOpenDate", "2025-05-01",
                                            "popupCloseDate", "2025-06-01",
                                            "address", "서울특별시 강남구 테헤란로 123, 3층",
                                            "latitude", "37.411222",
                                            "longitude", "126.999999"),
                                    Map.of(
                                            "popupId", 2,
                                            "popupName", "홍대 BTS 팝업스토어",
                                            "imageUrl", "https://bucket/bts.jpg",
                                            "popupOpenDate", "2025-05-15",
                                            "popupCloseDate", "2025-06-15",
                                            "address", "서울특별시 마포구 홍익로 123, 2층",
                                            "latitude", "37.511222",
                                            "longitude", "127.110011")));

            stubFindPopupsByArea(latMin, latMax, lngMin, lngMax, 200, expectedResponse);

            // when
            List<PopupMapResponse> result =
                    popupService.findPopupsByMapArea(latMin, latMax, lngMin, lngMax);

            // then
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).hasSize(2),
                    () -> assertThat(result.get(0).popupId()).isEqualTo(1L),
                    () -> assertThat(result.get(1).popupId()).isEqualTo(2L));
        }

        @Test
        void 지정된_영역_내에_팝업이_없으면_빈_리스트를_반환한다() throws JsonProcessingException {
            // given
            Double latMin = 37.378638;
            Double latMax = 37.671877;
            Double lngMin = 126.799543;
            Double lngMax = 127.184881;

            String expectedResponse = objectMapper.writeValueAsString(List.of());

            stubFindPopupsByArea(latMin, latMax, lngMin, lngMax, 200, expectedResponse);

            // when
            List<PopupMapResponse> result =
                    popupService.findPopupsByMapArea(latMin, latMax, lngMin, lngMax);

            // then
            Assertions.assertAll(
                    () -> assertThat(result).isNotNull(), () -> assertThat(result).isEmpty());
        }
    }

    private void stubFindHotPopupIds(int status, String body) {
        wireMockServer.stubFor(
                get(urlPathEqualTo("/internal/popups/popularity"))
                        .willReturn(
                                aResponse()
                                        .withStatus(status)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(body)));
    }

    private void stubFindHotPopupsByIds(
            PopupIdsRequest hotPopupIdsRequest, int status, String body) {
        try {
            String requestBody = objectMapper.writeValueAsString(hotPopupIdsRequest);

            wireMockServer.stubFor(
                    post(urlPathEqualTo("/internal/popups/popularity"))
                            .withRequestBody(equalToJson(requestBody))
                            .willReturn(
                                    aResponse()
                                            .withStatus(status)
                                            .withHeader(
                                                    "Content-Type",
                                                    MediaType.APPLICATION_JSON_VALUE)
                                            .withBody(body)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 실패", e);
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

    private void stubFindPopupsByName(String keyword, int size, int status, String body) {
        wireMockServer.stubFor(
                get(urlPathEqualTo("/internal/popups"))
                        .withQueryParam("keyword", equalTo(keyword))
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

    private void stubFindPopupsByArea(
            Double latMin, Double latMax, Double lngMin, Double lngMax, int status, String body) {
        wireMockServer.stubFor(
                get(urlPathEqualTo("/internal/popups/map"))
                        .withQueryParam("latMin", equalTo(String.valueOf(latMin)))
                        .withQueryParam("latMax", equalTo(String.valueOf(latMax)))
                        .withQueryParam("lngMin", equalTo(String.valueOf(lngMin)))
                        .withQueryParam("lngMax", equalTo(String.valueOf(lngMax)))
                        .willReturn(
                                aResponse()
                                        .withStatus(status)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(body)));
    }
}
