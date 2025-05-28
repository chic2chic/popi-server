package com.lgcns.service;

import com.lgcns.client.managerClient.ManagerServiceClient;
import com.lgcns.client.managerClient.dto.request.ItemIdsForPaymentRequest;
import com.lgcns.client.managerClient.dto.response.ItemForPaymentResponse;
import com.lgcns.client.memberClient.MemberServiceClient;
import com.lgcns.domain.Payment;
import com.lgcns.dto.request.PaymentReadyRequest;
import com.lgcns.dto.response.PaymentReadyResponse;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.PaymentErrorCode;
import com.lgcns.repository.PaymentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final MemberServiceClient memberServiceClient;
    private final ManagerServiceClient managerServiceClient;

    @Override
    public synchronized PaymentReadyResponse preparePayment(
            String memberId, PaymentReadyRequest request) {
        String buyerName = memberServiceClient.findMemberInfo(Long.valueOf(memberId)).nickname();

        List<Long> itemIds =
                request.items().stream().map(PaymentReadyRequest.Item::itemId).toList();

        List<ItemForPaymentResponse> itemDetails =
                managerServiceClient.findItemsForPayment(
                        request.popupId(), new ItemIdsForPaymentRequest(itemIds));

        List<String> itemNames = new ArrayList<>();
        int amount = 0;

        for (PaymentReadyRequest.Item selected : request.items()) {
            var item =
                    itemDetails.stream()
                            .filter(i -> i.itemId().equals(selected.itemId()))
                            .findFirst()
                            .orElseThrow(
                                    () -> new CustomException(PaymentErrorCode.ITEM_NOT_FOUND));

            if (selected.quantity() > item.stock()) {
                throw new CustomException(PaymentErrorCode.OUT_OF_STOCK);
            }

            amount += item.price() * selected.quantity();
            itemNames.add(item.name());
        }

        String name;
        int size = itemNames.size();

        if (size == 1) {
            name = itemNames.get(0);
        } else {
            name = String.format("%s 외 %d건", itemNames.get(0), size - 1);
        }

        String merchantUid =
                String.format("popup_%d_order_%s", request.popupId(), UUID.randomUUID());

        paymentRepository.save(Payment.createPayment(Long.valueOf(memberId), merchantUid, amount));

        return PaymentReadyResponse.of(buyerName, name, amount, merchantUid);
    }
}
