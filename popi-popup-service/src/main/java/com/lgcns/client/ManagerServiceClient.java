package com.lgcns.client;

import com.lgcns.config.FeignConfig;
import com.lgcns.dto.item.response.ItemInfoResponse;
import com.lgcns.dto.popup.response.PopupInfoResponse;
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

    @GetMapping("/internal/popups")
    SliceResponse<PopupInfoResponse> findAllPopups(
            @RequestParam(name = "lastPopupId", defaultValue = "0") Long lastPopupId,
            @RequestParam(name = "size", defaultValue = "8") int size);

    @GetMapping("/internal/popups/{popupId}/items")
    SliceResponse<ItemInfoResponse> findItemsByName(
            @PathVariable(name = "popupId") Long popupId,
            @RequestParam(name = "searchName", required = false) String searchName,
            @RequestParam(name = "lastItemId", required = false) Long lastItemId,
            @RequestParam(name = "size", defaultValue = "8") int size);
}
