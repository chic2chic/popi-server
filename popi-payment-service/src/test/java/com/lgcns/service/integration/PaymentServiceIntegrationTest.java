package com.lgcns.service.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.domain.Payment;
import com.lgcns.domain.PaymentItem;
import com.lgcns.domain.PaymentStatus;
import com.lgcns.dto.request.PaymentReadyRequest;
import com.lgcns.dto.response.AverageAmountResponse;
import com.lgcns.dto.response.ItemBuyerCountResponse;
import com.lgcns.dto.response.PaymentHistoryResponse;
import com.lgcns.dto.response.PaymentReadyResponse;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.PaymentErrorCode;
import com.lgcns.repository.PaymentRepository;
import com.lgcns.response.SliceResponse;
import com.lgcns.service.PaymentService;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.response.IamportResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class PaymentServiceIntegrationTest extends IntegrationTest {

    @Autowired private DatabaseCleaner databaseCleaner;

    @Autowired private PaymentService paymentService;
    @Autowired private PaymentRepository paymentRepository;

    @MockitoBean private IamportClient iamportClient;
    @Mock private IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse;
    @Mock private com.siot.IamportRestClient.response.Payment iamportPayment;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        databaseCleaner.execute();
    }

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
                                    .matches("^popup_1_order_[0-9a-fA-F\\-]{16}$"));
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

            stubItemDetails();

            // when & then
            assertThatThrownBy(() -> paymentService.preparePayment("1", request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.OUT_OF_STOCK.getMessage());
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
            stubIamportResponse(BigDecimal.valueOf(129000), "PAID");

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
            stubIamportResponse(BigDecimal.valueOf(1000), "PAID");

            // when & then
            assertThatThrownBy(() -> paymentService.findPaymentByImpUid("testImpUid"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.INVALID_AMOUNT.getMessage());
        }

        @Test
        void 결제_상태가_PAID가_아니면_예외가_발생한다() throws IOException, IamportResponseException {
            // given
            stubIamportResponse(BigDecimal.valueOf(129000), "READY");

            // when & then
            assertThatThrownBy(() -> paymentService.findPaymentByImpUid("testImpUid"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.NOT_PAID.getMessage());
        }

        private void stubIamportResponse(BigDecimal amount, String status)
                throws IOException, IamportResponseException {
            given(iamportClient.paymentByImpUid(anyString())).willReturn(iamportResponse);
            given(iamportResponse.getResponse()).willReturn(iamportPayment);

            given(iamportPayment.getMerchantUid()).willReturn("popup_1_order_test-uuid");
            given(iamportPayment.getPgProvider()).willReturn("tosspay");
            given(iamportPayment.getAmount()).willReturn(amount);
            given(iamportPayment.getStatus()).willReturn(status);
            given(iamportPayment.getPaidAt()).willReturn(new Date());
        }
    }

    @Nested
    class 관리자_서비스의_상품별_구매자_수_조회_요청을_처리할_때 {

        @BeforeEach
        void setUp() {
            Payment payment1 = Payment.createPayment(1L, "merchantUid1", 10000, 1L);
            payment1.updatePayment(
                    "impUid1",
                    "kakaopay",
                    PaymentStatus.PAID,
                    LocalDateTime.of(2025, 5, 31, 14, 14, 0));
            payment1.addPaymentItem(
                    PaymentItem.createPaymentItem(payment1, 1L, "DAZED 지수", 2, 15000));
            payment1.addPaymentItem(
                    PaymentItem.createPaymentItem(payment1, 2L, "DAZED 로제", 1, 15000));

            Payment payment2 = Payment.createPayment(2L, "merchantUid2", 20000, 1L);
            payment2.updatePayment(
                    "impUid2",
                    "tosspay",
                    PaymentStatus.PAID,
                    LocalDateTime.of(2025, 6, 1, 18, 0, 0));
            payment2.addPaymentItem(
                    PaymentItem.createPaymentItem(payment2, 1L, "DAZED 지수", 2, 15000));
            payment2.addPaymentItem(
                    PaymentItem.createPaymentItem(payment2, 3L, "DAZED 제니", 2, 9500));

            paymentRepository.saveAll(List.of(payment1, payment2));
        }

        @Test
        void 상품별_구매자_수를_정상적으로_조회한다() {
            // given
            Long popupId = 1L;

            // when
            List<ItemBuyerCountResponse> result = paymentService.countItemBuyerByPopupId(popupId);
            result.sort(Comparator.comparing(ItemBuyerCountResponse::itemId));

            // then
            Assertions.assertAll(
                    () -> assertThat(result.get(0).itemId()).isEqualTo(1L),
                    () -> assertThat(result.get(0).buyerCount()).isEqualTo(2),
                    () -> assertThat(result.get(1).itemId()).isEqualTo(2L),
                    () -> assertThat(result.get(1).buyerCount()).isEqualTo(1),
                    () -> assertThat(result.get(2).itemId()).isEqualTo(3L),
                    () -> assertThat(result.get(2).buyerCount()).isEqualTo(1));
        }
    }

    @Nested
    class 결제_내역을_조회할_때 {

        @BeforeEach
        void setUp() {
            Payment payment1 = Payment.createPayment(1L, "merchantUid1", 45000, 1L);
            payment1.updatePayment(
                    "impUid1",
                    "kakaopay",
                    PaymentStatus.PAID,
                    LocalDateTime.of(2024, 5, 31, 14, 0));
            payment1.addPaymentItem(PaymentItem.createPaymentItem(payment1, 1L, "응원봉", 1, 25000));
            payment1.addPaymentItem(PaymentItem.createPaymentItem(payment1, 2L, "포스터", 3, 9000));

            Payment payment2 = Payment.createPayment(1L, "merchantUid2", 12000, 2L);
            payment2.updatePayment(
                    "impUid2",
                    "tosspay",
                    PaymentStatus.PAID,
                    LocalDateTime.of(2024, 5, 30, 15, 30));
            payment2.addPaymentItem(
                    PaymentItem.createPaymentItem(payment2, 3L, "크레용 파란색", 1, 12000));

            paymentRepository.saveAll(List.of(payment1, payment2));
        }

        @Test
        void 결제별로_상품_목록이_포함된_내역이_정상적으로_조회된다() {
            // when
            SliceResponse<PaymentHistoryResponse> response =
                    paymentService.findAllPaymentHistory(String.valueOf(1L), null, 10);

            // then
            List<PaymentHistoryResponse> content = response.content();

            PaymentHistoryResponse first = content.get(0);

            Assertions.assertAll(
                    () -> assertThat(first.paymentId()).isEqualTo(1L),
                    () -> assertThat(first.popupId()).isEqualTo(1L),
                    () -> assertThat(first.items().get(0).itemName()).isEqualTo("응원봉"),
                    () -> assertThat(first.items().get(1).itemName()).isEqualTo("포스터"),
                    () -> assertThat(first.items().get(0).price()).isEqualTo(25000),
                    () -> assertThat(first.items().get(1).price()).isEqualTo(27000));

            PaymentHistoryResponse second = content.get(1);

            Assertions.assertAll(
                    () -> assertThat(second.paymentId()).isEqualTo(2L),
                    () -> assertThat(second.popupId()).isEqualTo(2L),
                    () -> assertThat(second.items().get(0).itemName()).isEqualTo("크레용 파란색"),
                    () -> assertThat(second.items().get(0).price()).isEqualTo(12000));
        }
    }

    @Nested
    class 관리자_서비스의_1인_평균_구매액_조회_요청을_처리할_때 {

        @BeforeEach
        void setUp() {
            Payment payment1 = Payment.createPayment(1L, "merchantUid1", 45000, 1L);
            payment1.updatePayment(
                    "impUid1",
                    "kakaopay",
                    PaymentStatus.PAID,
                    LocalDateTime.of(2025, 6, 1, 14, 14, 0));
            payment1.addPaymentItem(
                    PaymentItem.createPaymentItem(payment1, 1L, "DAZED 지수", 2, 15000));
            payment1.addPaymentItem(
                    PaymentItem.createPaymentItem(payment1, 2L, "DAZED 로제", 1, 15000));

            Payment payment2 = Payment.createPayment(2L, "merchantUid2", 34000, 1L);
            payment2.updatePayment("impUid2", "tosspay", PaymentStatus.PAID, LocalDateTime.now());
            payment2.addPaymentItem(
                    PaymentItem.createPaymentItem(payment2, 1L, "DAZED 지수", 2, 15000));
            payment2.addPaymentItem(
                    PaymentItem.createPaymentItem(payment2, 3L, "DAZED 제니", 2, 9500));

            Payment payment3 = Payment.createPayment(3L, "merchantUid3", 15000, 1L);
            payment3.updatePayment("impUid3", "kakaopay", PaymentStatus.PAID, LocalDateTime.now());
            payment3.addPaymentItem(
                    PaymentItem.createPaymentItem(payment3, 13L, "포스터 세트", 1, 15000));

            paymentRepository.saveAll(List.of(payment1, payment2, payment3));
        }

        @Test
        void 평균_구매액을_정상적으로_조회한다() {
            // given
            Long popupId = 1L;

            // when
            AverageAmountResponse response = paymentService.findAverageAmount(popupId);

            // then
            assertThat(response.totalAverageAmount()).isEqualTo(31333);
            assertThat(response.todayAverageAmount()).isEqualTo(24500);
        }
    }
}
