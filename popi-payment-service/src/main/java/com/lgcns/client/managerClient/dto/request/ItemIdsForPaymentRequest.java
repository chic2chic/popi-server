package com.lgcns.client.managerClient.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ItemIdsForPaymentRequest(
        @Schema(description = "결제할 아이템의 ID 리스트", example = "[1, 2, 4]") List<Long> itemIds) {}
