package com.lgcns.repository;

import static com.lgcns.domain.QPayment.payment;
import static com.lgcns.domain.QPaymentItem.paymentItem;

import com.lgcns.domain.PaymentStatus;
import com.lgcns.dto.response.AverageAmountResponse;
import com.lgcns.dto.response.ItemBuyerCountResponse;
import com.querydsl.core.Tuple;
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
        Tuple totalTuple =
                queryFactory
                        .select(payment.amount.sum(), payment.memberId.countDistinct())
                        .from(payment)
                        .where(payment.popupId.eq(popupId), payment.status.eq(PaymentStatus.PAID))
                        .fetchOne();

        Integer totalAmount = totalTuple != null ? totalTuple.get(0, Integer.class) : 0;
        Long totalBuyers = totalTuple != null ? totalTuple.get(1, Long.class) : 0L;

        Tuple todayTuple =
                queryFactory
                        .select(payment.amount.sum(), payment.memberId.countDistinct())
                        .from(payment)
                        .where(
                                payment.popupId.eq(popupId),
                                payment.status.eq(PaymentStatus.PAID),
                                payment.paidAt.goe(LocalDate.now().atStartOfDay()))
                        .fetchOne();

        Integer todayAmount = todayTuple != null ? todayTuple.get(0, Integer.class) : 0;
        Long todayBuyers = todayTuple != null ? todayTuple.get(1, Long.class) : 0L;

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
