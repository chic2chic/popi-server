package com.lgcns.client;

import com.lgcns.config.FeignConfig;
import com.lgcns.dto.response.PopupDetailsResponse;
import com.lgcns.dto.response.PopupInfoResponse;
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
    SliceResponse<PopupInfoResponse> findPopupsByName(
            @RequestParam(name = "searchName", required = false) String searchName,
            @RequestParam(name = "lastPopupId", required = false) Long lastPopupId,
            @RequestParam(name = "size", defaultValue = "8") int size);

    @GetMapping("/internal/popups/{popupId}")
    PopupDetailsResponse findPopupDetailsById(@PathVariable Long popupId);
}
