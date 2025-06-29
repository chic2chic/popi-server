package com.lgcns.service.unit;

import static com.lgcns.grpc.mapper.MemberGrpcMapper.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.lgcns.client.MemberGrpcClient;
import com.lgcns.client.managerClient.ManagerServiceClient;
import com.lgcns.client.managerClient.dto.request.ItemIdsForPaymentRequest;
import com.lgcns.client.managerClient.dto.response.ItemForPaymentResponse;
import com.lgcns.domain.Payment;
import com.lgcns.domain.PaymentStatus;
import com.lgcns.dto.FlatPaymentItem;
import com.lgcns.dto.request.PaymentReadyRequest;
import com.lgcns.dto.response.*;
import com.lgcns.enums.MemberAge;
import com.lgcns.enums.MemberGender;
import com.lgcns.enums.MemberRole;
import com.lgcns.enums.MemberStatus;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.PaymentErrorCode;
import com.lgcns.kafka.producer.ItemPurchasedProducer;
import com.lgcns.repository.PaymentRepository;
import com.lgcns.response.SliceResponse;
import com.lgcns.service.PaymentServiceImpl;
import com.popi.common.grpc.member.MemberInternalIdRequest;
import com.popi.common.grpc.member.MemberInternalInfoResponse;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.response.IamportResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceUnitTest {

    @InjectMocks private PaymentServiceImpl paymentService;
    @Mock private PaymentRepository paymentRepository;

    @Mock private MemberGrpcClient memberGrpcClient;
    @Mock private ManagerServiceClient managerServiceClient;

    @Mock private IamportClient iamportClient;
    @Mock private IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse;
    @Mock private com.siot.IamportRestClient.response.Payment iamportPayment;
    @Mock private ItemPurchasedProducer itemPurchasedProducer;

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

            stubFindMember();
            stubItemDetails();

            // when
            PaymentReadyResponse response = paymentService.preparePayment("1", request);

            // then
            verify(memberGrpcClient, times(1)).findByMemberId(any(MemberInternalIdRequest.class));
            verify(managerServiceClient, times(1))
                    .findItemsForPayment(anyLong(), any(ItemIdsForPaymentRequest.class));
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

            stubFindMember();
            stubItemDetails();

            // when & then
            assertThatThrownBy(() -> paymentService.preparePayment("1", request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.ITEM_NOT_FOUND.getMessage());
            verify(memberGrpcClient, times(1)).findByMemberId(any(MemberInternalIdRequest.class));
            verify(managerServiceClient, times(1))
                    .findItemsForPayment(anyLong(), any(ItemIdsForPaymentRequest.class));
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

            stubFindMember();
            stubItemDetails();

            // when & then
            assertThatThrownBy(() -> paymentService.preparePayment("1", request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.OUT_OF_STOCK.getMessage());
            verify(memberGrpcClient, times(1)).findByMemberId(any(MemberInternalIdRequest.class));
            verify(managerServiceClient, times(1))
                    .findItemsForPayment(anyLong(), any(ItemIdsForPaymentRequest.class));
        }

        private void stubFindMember() {
            given(memberGrpcClient.findByMemberId(any(MemberInternalIdRequest.class)))
                    .willReturn(
                            MemberInternalInfoResponse.newBuilder()
                                    .setMemberId(1L)
                                    .setNickname("현태")
                                    .setAge(toGrpcMemberAge(MemberAge.TWENTIES))
                                    .setGender(toGrpcMemberGender(MemberGender.MALE))
                                    .setRole(toGrpcMemberRole(MemberRole.USER))
                                    .setStatus(toGrpcMemberStatus(MemberStatus.NORMAL))
                                    .build());
        }

        private void stubItemDetails() {
            given(
                            managerServiceClient.findItemsForPayment(
                                    anyLong(), any(ItemIdsForPaymentRequest.class)))
                    .willReturn(
                            List.of(
                                    new ItemForPaymentResponse(9L, "피규어 지수", 84000, 20),
                                    new ItemForPaymentResponse(17L, "크룽크 미니백", 15000, 10)));
        }
    }

    @Nested
    class 결제_검증할_때 {

        @Test
        void impUid와_결제정보가_일치하면_결제정보를_업데이트한다() throws IOException, IamportResponseException {
            // given
            Payment payment = Payment.createPayment(1L, "popup_1_order_test-uuid", 129000, 1L);
            given(paymentRepository.findByMerchantUid("popup_1_order_test-uuid"))
                    .willReturn(Optional.of(payment));

            stubIamportResponse(BigDecimal.valueOf(129000), "PAID");

            // when
            paymentService.findPaymentByImpUid("testImpUid");

            // then
            Assertions.assertAll(
                    () -> assertThat(payment.getAmount()).isEqualTo(129000),
                    () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID),
                    () -> assertThat(payment.getMerchantUid()).isEqualTo("popup_1_order_test-uuid"),
                    () -> assertThat(payment.getPgProvider()).isEqualTo("tosspay"));
        }

        @Test
        void 결제_금액이_다르면_예외가_발생한다() throws IOException, IamportResponseException {
            // given
            Payment payment = Payment.createPayment(1L, "popup_1_order_test-uuid", 129000, 1L);
            given(paymentRepository.findByMerchantUid("popup_1_order_test-uuid"))
                    .willReturn(Optional.of(payment));

            stubIamportResponse(BigDecimal.valueOf(1000), "PAID");

            // when & then
            assertThatThrownBy(() -> paymentService.findPaymentByImpUid("testImpUid"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.INVALID_AMOUNT.getMessage());
        }

        @Test
        void 결제_상태가_PAID가_아니면_예외가_발생한다() throws IOException, IamportResponseException {
            // given
            Payment payment = Payment.createPayment(1L, "popup_1_order_test-uuid", 129000, 1L);
            given(paymentRepository.findByMerchantUid("popup_1_order_test-uuid"))
                    .willReturn(Optional.of(payment));

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

        @Test
        void 상품별_구매자_수를_정상적으로_조회한다() {
            // given
            Long popupId = 1L;

            given(paymentRepository.countItemBuyerByPopupId(popupId))
                    .willReturn(
                            List.of(
                                    new ItemBuyerCountResponse(1L, 2),
                                    new ItemBuyerCountResponse(2L, 1),
                                    new ItemBuyerCountResponse(3L, 1)));

            // when
            List<ItemBuyerCountResponse> result = paymentService.countItemBuyerByPopupId(popupId);

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

        @Test
        void 결제별로_상품_목록이_포함된_내역이_정상적으로_조회된다() {
            // given
            List<FlatPaymentItem> flatItems =
                    List.of(
                            new FlatPaymentItem(
                                    1L, 1L, LocalDateTime.of(2024, 5, 31, 14, 0), "응원봉", 1, 25000),
                            new FlatPaymentItem(
                                    1L, 1L, LocalDateTime.of(2024, 5, 31, 14, 0), "포스터", 3, 9000),
                            new FlatPaymentItem(
                                    2L,
                                    2L,
                                    LocalDateTime.of(2024, 5, 30, 15, 30),
                                    "크레용 파란색",
                                    1,
                                    12000));

            Slice<FlatPaymentItem> slice = new SliceImpl<>(flatItems, PageRequest.of(0, 10), false);

            given(paymentRepository.findAllPaymentHistoryByMemberId(anyLong(), any(), anyInt()))
                    .willReturn(slice);

            // when
            SliceResponse<PaymentHistoryResponse> response =
                    paymentService.findAllPaymentHistory("1", null, 10);

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

        @Test
        void 평균_구매액을_정상적으로_조회한다() {
            // given
            Long popupId = 1L;

            given(paymentRepository.findAverageAmountByPopupId(anyLong()))
                    .willReturn(new AverageAmountResponse(31333, 24500));

            // when
            AverageAmountResponse response = paymentService.findAverageAmount(popupId);

            // then
            assertThat(response.totalAverageAmount()).isEqualTo(31333);
            assertThat(response.todayAverageAmount()).isEqualTo(24500);
        }
    }
}
