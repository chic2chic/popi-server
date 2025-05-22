package com.lgcns.client;

import com.lgcns.client.dto.MonthlyReservationDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "popi-manager-service", url = "${member-service-url:}")
public interface ManagerServiceClient {
    @GetMapping("/reservations/popups/{popupId}")
    MonthlyReservationDto findMonthlyReservation(
            @PathVariable Long popupId, @RequestParam String date);
}
