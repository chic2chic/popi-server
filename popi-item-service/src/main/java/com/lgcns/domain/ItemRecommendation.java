package com.lgcns.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_recommendation_id")
    private Long id;

    private Long memberId;

    private Long reservationId;

    private Long itemId;

    private String itemName;

    private int itemPrice;

    private String itemImageUrl;

    @Builder(access = AccessLevel.PRIVATE)
    private ItemRecommendation(
            Long memberId,
            Long reservationId,
            Long itemId,
            String itemName,
            int itemPrice,
            String itemImageUrl) {
        this.memberId = memberId;
        this.reservationId = reservationId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemPrice = itemPrice;
        this.itemImageUrl = itemImageUrl;
    }

    public static ItemRecommendation createItemRecommendation(
            Long memberId,
            Long reservationId,
            Long itemId,
            String itemName,
            int itemPrice,
            String itemImageUrl) {
        return ItemRecommendation.builder()
                .memberId(memberId)
                .reservationId(reservationId)
                .itemId(itemId)
                .itemName(itemName)
                .itemPrice(itemPrice)
                .itemImageUrl(itemImageUrl)
                .build();
    }
}
