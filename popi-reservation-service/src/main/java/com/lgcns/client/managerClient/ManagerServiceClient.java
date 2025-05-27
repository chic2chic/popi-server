package com.lgcns.client.managerClient;

import com.lgcns.client.managerClient.dto.request.PopupIdsRequest;
import com.lgcns.client.managerClient.dto.response.MonthlyReservationResponse;
import com.lgcns.client.managerClient.dto.response.ReservationPopupInfoResponse;
import com.lgcns.config.FeignConfig;
import com.lgcns.dto.response.SurveyChoiceResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/internal/reservations")
    List<ReservationPopupInfoResponse> findReservedPopupInfo(@RequestBody PopupIdsRequest request);
}
