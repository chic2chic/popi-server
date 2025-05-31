package com.lgcns.kafka.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lgcns.domain.Payment;
import java.time.LocalDateTime;
import java.util.List;

public record ItemPurchasedMessage(
        Long popupId,
        List<Item> items,
        int amount,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
                LocalDateTime purchasedAt) {
    public static ItemPurchasedMessage from(Payment payment) {
        List<Item> items =
                payment.getItems().stream()
                        .map(item -> new Item(item.getItemId(), item.getQuantity()))
                        .toList();

        return new ItemPurchasedMessage(
                payment.getPopupId(), items, payment.getAmount(), payment.getUpdatedAt());
    }

    public record Item(Long itemId, Integer quantity) {}
}
