package com.lgcns.client.managerClient;

import com.lgcns.config.FeignConfig;
import com.lgcns.dto.response.PopupDetailsResponse;
import com.lgcns.dto.response.PopupInfoResponse;
import com.lgcns.response.SliceResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "${manager.service.name}",
        url = "${manager.service.url:}",
        configuration = FeignConfig.class)
public interface ManagerServiceClient {

    @GetMapping("/internal/popups")
    SliceResponse<PopupInfoResponse> findPopupsByName(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "lastPopupId", required = false) Long lastPopupId,
            @RequestParam(name = "size", defaultValue = "8") int size);

    @GetMapping("/internal/popups/{popupId}")
    PopupDetailsResponse findPopupDetailsById(@PathVariable(name = "popupId") Long popupId);

    @PostMapping("/internal/popups/popularity")
    List<PopupInfoResponse> findHotPopupsByIds(@Valid @RequestBody PopupIdsRequest request);
}
