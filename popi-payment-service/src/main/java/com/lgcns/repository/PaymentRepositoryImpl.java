package com.lgcns.repository;

import static com.lgcns.domain.QPayment.payment;
import static com.lgcns.domain.QPaymentItem.paymentItem;

import com.lgcns.domain.PaymentStatus;
import com.lgcns.dto.FlatPaymentItem;
import com.lgcns.dto.response.AverageAmountResponse;
import com.lgcns.dto.response.ItemBuyerCountResponse;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
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

    @Override
    public Slice<FlatPaymentItem> findAllPaymentHistoryByMemberId(
            Long memberId, Long lastPaymentId, int size) {
        List<Long> paymentIds =
                queryFactory
                        .select(payment.id)
                        .from(payment)
                        .where(
                                payment.memberId.eq(memberId),
                                payment.status.eq(PaymentStatus.PAID),
                                lastPaymentCondition(lastPaymentId))
                        .orderBy(payment.paidAt.desc(), payment.id.desc())
                        .limit(size + 1L)
                        .fetch();

        boolean hasNext = false;
        if (paymentIds.size() > size) {
            hasNext = true;
            paymentIds.remove(size);
        }

        List<FlatPaymentItem> results =
                queryFactory
                        .select(
                                Projections.constructor(
                                        FlatPaymentItem.class,
                                        payment.id,
                                        payment.popupId,
                                        payment.paidAt,
                                        paymentItem.name,
                                        paymentItem.quantity,
                                        paymentItem.price))
                        .from(payment)
                        .join(paymentItem)
                        .on(payment.id.eq(paymentItem.payment.id))
                        .where(payment.id.in(paymentIds))
                        .orderBy(payment.paidAt.desc(), payment.id.desc())
                        .fetch();

        return new SliceImpl<>(results, PageRequest.of(0, size), hasNext);
    }

    private BooleanExpression lastPaymentCondition(Long paymentId) {
        return (paymentId != null) ? payment.id.lt(paymentId) : null;
    }
}
