package com.lgcns.service;

import com.lgcns.client.managerClient.ManagerServiceClient;
import com.lgcns.client.managerClient.dto.request.ItemIdsForPaymentRequest;
import com.lgcns.client.managerClient.dto.response.ItemForPaymentResponse;
import com.lgcns.client.memberClient.MemberServiceClient;
import com.lgcns.domain.Payment;
import com.lgcns.domain.PaymentItem;
import com.lgcns.domain.PaymentStatus;
import com.lgcns.dto.FlatPaymentItem;
import com.lgcns.dto.request.PaymentReadyRequest;
import com.lgcns.dto.response.AverageAmountResponse;
import com.lgcns.dto.response.ItemBuyerCountResponse;
import com.lgcns.dto.response.PaymentHistoryResponse;
import com.lgcns.dto.response.PaymentReadyResponse;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.PaymentErrorCode;
import com.lgcns.kafka.message.ItemPurchasedMessage;
import com.lgcns.kafka.producer.ItemPurchasedProducer;
import com.lgcns.repository.PaymentRepository;
import com.lgcns.response.SliceResponse;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.response.IamportResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final MemberServiceClient memberServiceClient;
    private final ManagerServiceClient managerServiceClient;
    private final IamportClient iamportClient;
    private final ItemPurchasedProducer itemPurchasedProducer;

    @Override
    public synchronized PaymentReadyResponse preparePayment(
            String memberId, PaymentReadyRequest request) {
        String buyerName = memberServiceClient.findMemberInfo(Long.valueOf(memberId)).nickname();

        List<Long> itemIds =
                request.items().stream().map(PaymentReadyRequest.Item::itemId).toList();

        List<ItemForPaymentResponse> itemDetails =
                managerServiceClient.findItemsForPayment(
                        request.popupId(), new ItemIdsForPaymentRequest(itemIds));

        Map<Long, ItemForPaymentResponse> itemDetailMap =
                itemDetails.stream()
                        .collect(
                                Collectors.toMap(
                                        ItemForPaymentResponse::itemId, Function.identity()));

        List<String> itemNames = new ArrayList<>();
        int amount = 0;

        for (PaymentReadyRequest.Item selected : request.items()) {
            ItemForPaymentResponse item = itemDetailMap.get(selected.itemId());

            if (item == null) {
                throw new CustomException(PaymentErrorCode.ITEM_NOT_FOUND);
            }

            if (selected.quantity() > item.stock()) {
                throw new CustomException(PaymentErrorCode.OUT_OF_STOCK);
            }

            amount += item.price() * selected.quantity();
            itemNames.add(item.name());
        }

        String name =
                itemNames.size() == 1
                        ? itemNames.get(0)
                        : String.format("%s 외 %d건", itemNames.get(0), itemNames.size() - 1);

        String merchantUid =
                String.format(
                        "popup_%d_order_%s",
                        request.popupId(), UUID.randomUUID().toString().substring(0, 16));

        Payment payment =
                Payment.createPayment(
                        Long.valueOf(memberId), merchantUid, amount, request.popupId());

        for (PaymentReadyRequest.Item selected : request.items()) {
            ItemForPaymentResponse item = itemDetailMap.get(selected.itemId());

            payment.addPaymentItem(
                    PaymentItem.createPaymentItem(
                            payment,
                            selected.itemId(),
                            item.name(),
                            selected.quantity(),
                            item.price()));
        }

        paymentRepository.save(payment);

        return PaymentReadyResponse.of(buyerName, name, amount, merchantUid);
    }

    @Override
    public void findPaymentByImpUid(String impUid) throws IamportResponseException, IOException {
        try {
            IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse =
                    iamportClient.paymentByImpUid(impUid);
            com.siot.IamportRestClient.response.Payment iamportPayment =
                    iamportResponse.getResponse();

            String merchantUid = iamportPayment.getMerchantUid();
            String pgProvider = iamportPayment.getPgProvider();
            int amount = iamportPayment.getAmount().intValue();
            PaymentStatus status = PaymentStatus.valueOf(iamportPayment.getStatus().toUpperCase());
            LocalDateTime paidAt =
                    iamportPayment
                            .getPaidAt()
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();

            Payment payment =
                    paymentRepository
                            .findByMerchantUid(merchantUid)
                            .orElseThrow(
                                    () -> new CustomException(PaymentErrorCode.PAYMENT_NOT_FOUND));

            if (amount != payment.getAmount()) {
                throw new CustomException(PaymentErrorCode.INVALID_AMOUNT);
            }

            if (status != PaymentStatus.PAID) {
                throw new CustomException(PaymentErrorCode.NOT_PAID);
            }

            payment.updatePayment(impUid, pgProvider, PaymentStatus.PAID, paidAt);

            itemPurchasedProducer.sendMessage(ItemPurchasedMessage.from(payment));
        } catch (IamportResponseException e) {
            log.error(
                    "Iamport API 오류: status={}, message={}", e.getHttpStatusCode(), e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemBuyerCountResponse> countItemBuyerByPopupId(Long popupId) {
        return paymentRepository.countItemBuyerByPopupId(popupId);
    }

    @Override
    @Transactional(readOnly = true)
    public AverageAmountResponse findAverageAmount(Long popupId) {
        return paymentRepository.findAverageAmountByPopupId(popupId);
    }

    @Override
    @Transactional(readOnly = true)
    public SliceResponse<PaymentHistoryResponse> findAllPaymentHistory(
            String memberId, Long lastPaymentId, int size) {
        Slice<FlatPaymentItem> flatPaymentItems =
                paymentRepository.findAllPaymentHistoryByMemberId(
                        Long.valueOf(memberId), lastPaymentId, size);

        List<PaymentHistoryResponse> results = groupByPayment(flatPaymentItems.getContent());

        return SliceResponse.from(
                new SliceImpl<>(
                        results, flatPaymentItems.getPageable(), flatPaymentItems.hasNext()));
    }

    private List<PaymentHistoryResponse> groupByPayment(List<FlatPaymentItem> flatPaymentItems) {
        Map<Long, List<FlatPaymentItem>> paymentGroups =
                flatPaymentItems.stream()
                        .collect(
                                Collectors.groupingBy(
                                        FlatPaymentItem::paymentId,
                                        LinkedHashMap::new,
                                        Collectors.toList()));

        return paymentGroups.values().stream().map(this::toPaymentResponse).toList();
    }

    private PaymentHistoryResponse toPaymentResponse(List<FlatPaymentItem> flatPaymentItems) {
        FlatPaymentItem base = flatPaymentItems.get(0);

        List<PaymentHistoryResponse.Item> itemDetails =
                flatPaymentItems.stream()
                        .map(
                                item ->
                                        new PaymentHistoryResponse.Item(
                                                item.itemName(),
                                                item.quantity(),
                                                item.price() * item.quantity()))
                        .toList();

        return new PaymentHistoryResponse(
                base.paymentId(), base.popupId(), base.paidAt(), itemDetails);
    }
}
