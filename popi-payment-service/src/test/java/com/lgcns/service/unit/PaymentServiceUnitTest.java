package com.lgcns.service.unit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import com.lgcns.client.managerClient.ManagerServiceClient;
import com.lgcns.client.managerClient.dto.request.ItemIdsForPaymentRequest;
import com.lgcns.client.managerClient.dto.response.ItemForPaymentResponse;
import com.lgcns.client.memberClient.MemberServiceClient;
import com.lgcns.domain.Payment;
import com.lgcns.domain.PaymentStatus;
import com.lgcns.dto.request.PaymentReadyRequest;
import com.lgcns.dto.response.MemberInternalInfoResponse;
import com.lgcns.dto.response.PaymentReadyResponse;
import com.lgcns.enums.MemberAge;
import com.lgcns.enums.MemberGender;
import com.lgcns.enums.MemberRole;
import com.lgcns.enums.MemberStatus;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.PaymentErrorCode;
import com.lgcns.kafka.producer.ItemPurchasedProducer;
import com.lgcns.repository.PaymentRepository;
import com.lgcns.service.PaymentServiceImpl;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.response.IamportResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceUnitTest {

    @InjectMocks private PaymentServiceImpl paymentService;
    @Mock private PaymentRepository paymentRepository;

    @Mock private MemberServiceClient memberServiceClient;
    @Mock private ManagerServiceClient managerServiceClient;

    @Mock IamportClient iamportClient;
    @Mock IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse;
    @Mock com.siot.IamportRestClient.response.Payment iamportPayment;
    @Mock ItemPurchasedProducer itemPurchasedProducer;

    @Nested
    class 결제_준비할_때 {

        @Test
        void 유효한_요청이라면_결제_준비_정보를_응답한다() {
            // given
            PaymentReadyRequest request =
                    new PaymentReadyRequest(
                            1L,
                            List.of(
                                    new PaymentReadyRequest.Item(9L, 1),
                                    new PaymentReadyRequest.Item(17L, 3)));

            stubMemberInfo();
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
        void 존재하지_않는_itemId가_포함되어_있다면_예외가_발생한다() {
            // given
            PaymentReadyRequest request =
                    new PaymentReadyRequest(
                            1L,
                            List.of(
                                    new PaymentReadyRequest.Item(9L, 1),
                                    new PaymentReadyRequest.Item(999L, 3)));

            stubMemberInfo();
            stubItemDetails();

            // when & then
            assertThatThrownBy(() -> paymentService.preparePayment("1", request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.ITEM_NOT_FOUND.getMessage());
        }

        @Test
        void 재고보다_많은_수량을_요청하면_OUT_OF_STOCK_예외가_발생한다() {
            // given
            PaymentReadyRequest request =
                    new PaymentReadyRequest(
                            1L,
                            List.of(
                                    new PaymentReadyRequest.Item(9L, 1),
                                    new PaymentReadyRequest.Item(17L, 12)));

            stubMemberInfo();
            stubItemDetails();

            // when & then
            assertThatThrownBy(() -> paymentService.preparePayment("1", request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.OUT_OF_STOCK.getMessage());
        }

        private void stubMemberInfo() {
            when(memberServiceClient.findMemberInfo(anyLong()))
                    .thenReturn(
                            new MemberInternalInfoResponse(
                                    1L,
                                    "현태",
                                    MemberAge.TWENTIES,
                                    MemberGender.MALE,
                                    MemberRole.USER,
                                    MemberStatus.NORMAL));
        }

        private void stubItemDetails() {
            when(managerServiceClient.findItemsForPayment(
                            anyLong(), any(ItemIdsForPaymentRequest.class)))
                    .thenReturn(
                            List.of(
                                    new ItemForPaymentResponse(9L, "피규어 지수", 84000, 20),
                                    new ItemForPaymentResponse(17L, "크룽크 미니백", 15000, 10)));
        }
    }

    @Nested
    class 결제_검증할_때 {

        private final String merchantUid = "popup_1_order_test-uuid";
        private final BigDecimal amount = BigDecimal.valueOf(129000);

        @BeforeEach
        void setUp() {
            Payment payment = Payment.createPayment(1L, merchantUid, 129000, 1L);
            when(paymentRepository.findByMerchantUid(merchantUid)).thenReturn(Optional.of(payment));
        }

        @Test
        void impUid와_결제정보가_일치하면_결제정보를_업데이트한다() throws IOException, IamportResponseException {
            // given
            when(iamportClient.paymentByImpUid(anyString())).thenReturn(iamportResponse);
            when(iamportResponse.getResponse()).thenReturn(iamportPayment);

            when(iamportPayment.getMerchantUid()).thenReturn(merchantUid);
            when(iamportPayment.getPgProvider()).thenReturn("tosspay");
            when(iamportPayment.getAmount()).thenReturn(amount);
            when(iamportPayment.getStatus()).thenReturn("PAID");
            when(iamportPayment.getPaidAt()).thenReturn(new Date());

            // when
            paymentService.findPaymentByImpUid("testImpUid");

            // then
            Payment payment =
                    paymentRepository.findByMerchantUid("popup_1_order_test-uuid").orElseThrow();
            Assertions.assertAll(
                    () -> assertThat(payment.getAmount()).isEqualTo(129000),
                    () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID),
                    () -> assertThat(payment.getMerchantUid()).isEqualTo(merchantUid),
                    () -> assertThat(payment.getPgProvider()).isEqualTo("tosspay"));
        }

        @Test
        void 결제_금액이_다르면_예외가_발생한다() throws IOException, IamportResponseException {
            // given
            when(iamportClient.paymentByImpUid(anyString())).thenReturn(iamportResponse);
            when(iamportResponse.getResponse()).thenReturn(iamportPayment);

            when(iamportPayment.getMerchantUid()).thenReturn(merchantUid);
            when(iamportPayment.getPgProvider()).thenReturn("tosspay");
            when(iamportPayment.getAmount()).thenReturn(BigDecimal.valueOf(1000));
            when(iamportPayment.getStatus()).thenReturn("PAID");
            when(iamportPayment.getPaidAt()).thenReturn(new Date());

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

            when(iamportPayment.getMerchantUid()).thenReturn(merchantUid);
            when(iamportPayment.getPgProvider()).thenReturn("tosspay");
            when(iamportPayment.getAmount()).thenReturn(amount);
            when(iamportPayment.getStatus()).thenReturn("READY");
            when(iamportPayment.getPaidAt()).thenReturn(new Date());

            // when & then
            assertThatThrownBy(() -> paymentService.findPaymentByImpUid("testImpUid"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.NOT_PAID.getMessage());
        }
    }
}
