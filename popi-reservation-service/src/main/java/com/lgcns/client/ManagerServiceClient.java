package com.lgcns.client;

import com.lgcns.client.dto.MonthlyReservationResponse;
import com.lgcns.config.FeignConfig;
import com.lgcns.dto.response.SurveyChoiceResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "${manager.service.name}",
        url = "${manager.service.url:}",
        configuration = FeignConfig.class)
public interface ManagerServiceClient {
    @GetMapping("/internal/reservations/popups/{popupId}")
    MonthlyReservationResponse findMonthlyReservation(
            @PathVariable Long popupId, @RequestParam String date);

    @GetMapping("/internal/reservations/popups/{popupId}/survey")
    List<SurveyChoiceResponse> findSurveyChoicesByPopupId(@PathVariable Long popupId);
}
