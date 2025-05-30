package com.lgcns.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.WireMockIntegrationTest;
import com.lgcns.domain.Payment;
import com.lgcns.domain.PaymentStatus;
import com.lgcns.dto.request.PaymentReadyRequest;
import com.lgcns.dto.response.PaymentReadyResponse;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.PaymentErrorCode;
import com.lgcns.repository.PaymentRepository;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.response.IamportResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class PaymentServiceTest extends WireMockIntegrationTest {

    @Autowired PaymentService paymentService;
    @Autowired PaymentRepository paymentRepository;

    @MockitoBean IamportClient iamportClient;
    @Mock IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse;
    @Mock com.siot.IamportRestClient.response.Payment iamportPayment;

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

    @Nested
    class 결제_검증할_때 {

        @BeforeEach
        void setUp() {
            paymentRepository.deleteAll();
            paymentRepository.save(
                    Payment.createPayment(1L, "popup_1_order_test-uuid", 129000, 1L));
        }

        @Test
        void impUid와_결제정보가_일치하면_결제정보를_업데이트한다() throws IOException, IamportResponseException {
            // given
            when(iamportClient.paymentByImpUid(anyString())).thenReturn(iamportResponse);
            when(iamportResponse.getResponse()).thenReturn(iamportPayment);

            when(iamportPayment.getMerchantUid()).thenReturn("popup_1_order_test-uuid");
            when(iamportPayment.getPgProvider()).thenReturn("tosspay");
            when(iamportPayment.getAmount()).thenReturn(BigDecimal.valueOf(129000));
            when(iamportPayment.getStatus()).thenReturn("PAID");

            // when
            paymentService.findPaymentByImpUid("testImpUid");

            // then
            Payment payment = paymentRepository.findByMerchantUid("popup_1_order_test-uuid").get();
            Assertions.assertAll(
                    () -> assertThat(payment.getAmount()).isEqualTo(129000),
                    () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID),
                    () -> assertThat(payment.getMerchantUid()).isEqualTo("popup_1_order_test-uuid"),
                    () -> assertThat(payment.getPgProvider()).isEqualTo("tosspay"));
        }

        @Test
        void 결제_금액이_다르면_예외가_발생한다() throws IOException, IamportResponseException {
            // given
            when(iamportClient.paymentByImpUid(anyString())).thenReturn(iamportResponse);
            when(iamportResponse.getResponse()).thenReturn(iamportPayment);

            when(iamportPayment.getMerchantUid()).thenReturn("popup_1_order_test-uuid");
            when(iamportPayment.getPgProvider()).thenReturn("tosspay");
            when(iamportPayment.getAmount()).thenReturn(BigDecimal.valueOf(1000));
            when(iamportPayment.getStatus()).thenReturn("PAID");

            // when & then
            assertThatThrownBy(() -> paymentService.findPaymentByImpUid("testImpUid"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.INVALID_AMOUNT.getMessage());
        }

        @Test
        void 결제_상태가_PAID가_아니면_예외가_발생한다() throws IOException, IamportResponseException {
            // given
            when(iamportClient.paymentByImpUid(anyString())).thenReturn(iamportResponse);
            when(iamportResponse.getResponse()).thenReturn(iamportPayment);

            when(iamportPayment.getMerchantUid()).thenReturn("popup_1_order_test-uuid");
            when(iamportPayment.getPgProvider()).thenReturn("tosspay");
            when(iamportPayment.getAmount()).thenReturn(BigDecimal.valueOf(129000));
            when(iamportPayment.getStatus()).thenReturn("READY");

            // when & then
            assertThatThrownBy(() -> paymentService.findPaymentByImpUid("testImpUid"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.NOT_PAID.getMessage());
        }
    }
}
