package com.lgcns.repository;

import static com.lgcns.domain.QPayment.payment;
import static com.lgcns.domain.QPaymentItem.paymentItem;

import com.lgcns.domain.PaymentStatus;
import com.lgcns.dto.response.ItemBuyerCountResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ItemBuyerCountResponse> countItemBuyerByPopupId(Long popupId) {
        return queryFactory
                .select(
                        Projections.constructor(
                                ItemBuyerCountResponse.class,
                                paymentItem.itemId,
                                payment.memberId.countDistinct()))
                .from(paymentItem)
                .join(paymentItem.payment, payment)
                .where(payment.popupId.eq(popupId), payment.status.eq(PaymentStatus.PAID))
                .groupBy(paymentItem.itemId)
                .fetch();
    }
}
