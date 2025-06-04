package com.lgcns.domain;

import com.lgcns.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    private Long itemId;

    private String name;

    private int quantity;

    private int price;

    @Builder
    private PaymentItem(Payment payment, Long itemId, String name, int quantity, int price) {
        this.payment = payment;
        this.itemId = itemId;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    public static PaymentItem createPaymentItem(
            Payment payment, Long itemId, String name, int quantity, int price) {
        return PaymentItem.builder()
                .payment(payment)
                .itemId(itemId)
                .name(name)
                .quantity(quantity)
                .price(price)
                .build();
    }

    public void updatePayment(Payment payment) {
        this.payment = payment;
    }
}
