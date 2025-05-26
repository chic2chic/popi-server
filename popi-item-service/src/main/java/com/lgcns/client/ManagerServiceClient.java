package com.lgcns.client;

import com.lgcns.config.FeignConfig;
import com.lgcns.dto.response.ItemInfoResponse;
import com.lgcns.response.SliceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "${manager.service.name}",
        url = "${manager.service.url:}",
        configuration = FeignConfig.class)
public interface ManagerServiceClient {

    @GetMapping("/internal/popups/{popupId}/items")
    SliceResponse<ItemInfoResponse> findItemsByName(
            @PathVariable(name = "popupId") Long popupId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "lastItemId", required = false) Long lastItemId,
            @RequestParam(name = "size", defaultValue = "8") int size);
}
