package com.lgcns.kafka.message;

import com.lgcns.domain.Payment;
import java.util.List;

public record ItemPurchasedMessage(List<Item> items) {
    public static ItemPurchasedMessage from(Payment payment) {
        List<Item> items =
                payment.getItems().stream()
                        .map(item -> new Item(item.getItemId(), item.getQuantity()))
                        .toList();

        return new ItemPurchasedMessage(items);
    }

    public record Item(Long itemId, Integer quantity) {}
}
