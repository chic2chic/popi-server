package com.lgcns.dto;

import java.time.LocalDateTime;

public record FlatPaymentItem(
        Long paymentId,
        Long popupId,
        LocalDateTime paidAt,
        String itemName,
        int quantity,
        int price) {}
