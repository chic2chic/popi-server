package com.lgcns.client;

import com.lgcns.client.dto.UpcomingReservationResponse;
import com.lgcns.config.FeignConfig;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "${reservation.service.name}",
        url = "${reservation.service.url:}",
        configuration = FeignConfig.class)
public interface ReservationServiceClient {

    @GetMapping("/internal/upcoming")
    List<UpcomingReservationResponse> findUpcomingReservations();
}
