package com.lgcns.client;

import com.lgcns.config.FeignConfig;
import com.lgcns.dto.response.PopupInfoResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "${reservation.service.name}",
        url = "${reservation.service.url:}",
        configuration = FeignConfig.class)
public interface ReservationServiceClient {

    @GetMapping("/internal/popups/hot")
    List<PopupInfoResponse> findHotPopups();
}
