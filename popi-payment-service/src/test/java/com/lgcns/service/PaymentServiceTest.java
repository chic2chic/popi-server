package com.lgcns.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.WireMockIntegrationTest;
import com.lgcns.dto.request.PaymentReadyRequest;
import com.lgcns.dto.response.PaymentReadyResponse;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.PaymentErrorCode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class PaymentServiceTest extends WireMockIntegrationTest {

    @Autowired PaymentService paymentService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    class 결제_준비할_때 {

        @Test
        void 유효한_요청이라면_결제_준비_정보를_응답한다() throws JsonProcessingException {
            // given
            PaymentReadyRequest request =
                    new PaymentReadyRequest(
                            1L,
                            List.of(
                                    new PaymentReadyRequest.Item(9L, 1),
                                    new PaymentReadyRequest.Item(17L, 3)));

            stubManagerInfo();
            stubItemDetails();

            // when
            PaymentReadyResponse response = paymentService.preparePayment("1", request);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.buyerName()).isEqualTo("현태"),
                    () -> assertThat(response.name()).isEqualTo("피규어 지수 외 1건"),
                    () -> assertThat(response.amount()).isEqualTo(129000),
                    () ->
                            assertThat(response.merchantUid())
                                    .matches("^popup_1_order_[0-9a-fA-F\\-]{36}$"));
        }

        @Test
        void 존재하지_않는_itemId가_포함되어_있다면_예외가_발생한다() throws JsonProcessingException {
            // given
            PaymentReadyRequest request =
                    new PaymentReadyRequest(
                            1L,
                            List.of(
                                    new PaymentReadyRequest.Item(9L, 1),
                                    new PaymentReadyRequest.Item(999L, 3)));

            stubManagerInfo();
            stubItemDetails();

            // when & then
            assertThatThrownBy(() -> paymentService.preparePayment("1", request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.ITEM_NOT_FOUND.getMessage());
        }

        @Test
        void 재고보다_많은_수량을_요청하면_OUT_OF_STOCK_예외가_발생한다() throws JsonProcessingException {
            // given
            PaymentReadyRequest request =
                    new PaymentReadyRequest(
                            1L,
                            List.of(
                                    new PaymentReadyRequest.Item(9L, 1),
                                    new PaymentReadyRequest.Item(17L, 12)));

            stubManagerInfo();
            stubItemDetails();

            // when & then
            assertThatThrownBy(() -> paymentService.preparePayment("1", request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.OUT_OF_STOCK.getMessage());
        }

        private void stubManagerInfo() throws JsonProcessingException {
            String managerExpectedResponse =
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "memberId",
                                    1,
                                    "nickname",
                                    "현태",
                                    "role",
                                    "USER",
                                    "status",
                                    "NORMAL"));

            stubFor(
                    get(urlEqualTo("/internal/1"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(managerExpectedResponse)));
        }

        private void stubItemDetails() throws JsonProcessingException {
            String body =
                    objectMapper.writeValueAsString(
                            List.of(
                                    Map.of(
                                            "itemId", 9, "name", "피규어 지수", "price", 84000, "stock",
                                            20),
                                    Map.of(
                                            "itemId", 17, "name", "크룽크 미니백", "price", 15000,
                                            "stock", 10)));

            stubFor(
                    post(urlEqualTo("/internal/popups/1/items/details"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(body)));
        }
    }
}
