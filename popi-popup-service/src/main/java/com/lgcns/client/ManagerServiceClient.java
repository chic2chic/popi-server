package com.lgcns.client;

import com.lgcns.dto.item.response.ItemInfoResponse;
import com.lgcns.response.SliceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "popi-manager-service", url = "${manager-service-url:}")
public interface ManagerServiceClient {

    @GetMapping("/internal/popups/{popupId}/items")
    SliceResponse<ItemInfoResponse> findAllItems(
            @PathVariable(name = "popupId") Long popupId,
            @RequestParam(name = "lastItemId", required = false) Long lastItemId,
            @RequestParam(name = "size", defaultValue = "8") int size);
}
