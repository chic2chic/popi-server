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
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    private Long memberId;
    private String merchantUid;
    private String impUid;

    private String pgProvider;
    private int amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Builder(access = AccessLevel.PRIVATE)
    private Payment(Long memberId, String merchantUid, int amount, PaymentStatus status) {
        this.memberId = memberId;
        this.merchantUid = merchantUid;
        this.amount = amount;
        this.status = status;
    }

    public static Payment createPayment(Long memberId, String merchantUid, int amount) {
        return Payment.builder()
                .memberId(memberId)
                .merchantUid(merchantUid)
                .amount(amount)
                .status(PaymentStatus.READY)
                .build();
    }
}
