package com.lgcns.repository;

import static com.lgcns.domain.QPayment.payment;
import static com.lgcns.domain.QPaymentItem.paymentItem;

import com.lgcns.domain.PaymentStatus;
import com.lgcns.dto.response.AverageAmountResponse;
import com.lgcns.dto.response.ItemBuyerCountResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
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

    @Override
    public AverageAmountResponse findAverageAmountByPopupId(Long popupId) {
        Integer totalAmount =
                queryFactory
                        .select(payment.amount.sum())
                        .from(payment)
                        .where(payment.popupId.eq(popupId), payment.status.eq(PaymentStatus.PAID))
                        .fetchOne();

        Long totalBuyers =
                queryFactory
                        .select(payment.memberId.countDistinct())
                        .from(payment)
                        .where(payment.popupId.eq(popupId), payment.status.eq(PaymentStatus.PAID))
                        .fetchOne();

        Integer todayAmount =
                queryFactory
                        .select(payment.amount.sum())
                        .from(payment)
                        .where(
                                payment.popupId.eq(popupId),
                                payment.status.eq(PaymentStatus.PAID),
                                payment.updatedAt.goe(LocalDate.now().atStartOfDay()))
                        .fetchOne();

        Long todayBuyers =
                queryFactory
                        .select(payment.memberId.countDistinct())
                        .from(payment)
                        .where(
                                payment.popupId.eq(popupId),
                                payment.status.eq(PaymentStatus.PAID),
                                payment.updatedAt.goe(LocalDate.now().atStartOfDay()))
                        .fetchOne();

        int totalAverageAmount =
                (totalBuyers == null || totalBuyers == 0)
                        ? 0
                        : (totalAmount != null ? totalAmount : 0) / totalBuyers.intValue();
        int todayAverageAmount =
                (todayBuyers == null || todayBuyers == 0)
                        ? 0
                        : (todayAmount != null ? todayAmount : 0) / todayBuyers.intValue();

        return AverageAmountResponse.of(totalAverageAmount, todayAverageAmount);
    }
}
