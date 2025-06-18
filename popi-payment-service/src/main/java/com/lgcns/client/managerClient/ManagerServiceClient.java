package com.lgcns.client.managerClient;

import com.lgcns.client.managerClient.dto.request.ItemIdsForPaymentRequest;
import com.lgcns.client.managerClient.dto.response.ItemForPaymentResponse;
import com.lgcns.config.FeignConfig;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "${manager.service.name:}",
        url = "${manager.service.url:}",
        configuration = FeignConfig.class)
public interface ManagerServiceClient {
    @PostMapping("/internal/popups/{popupId}/items/details")
    List<ItemForPaymentResponse> findItemsForPayment(
            @PathVariable Long popupId, @RequestBody ItemIdsForPaymentRequest request);
}
