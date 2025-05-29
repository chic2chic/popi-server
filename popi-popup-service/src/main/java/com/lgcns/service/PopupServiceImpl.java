package com.lgcns.service;

import com.lgcns.client.managerClient.ManagerServiceClient;
import com.lgcns.client.managerClient.dto.PopupIdsRequest;
import com.lgcns.client.reservationClient.ReservationServiceClient;
import com.lgcns.dto.response.PopupDetailsResponse;
import com.lgcns.dto.response.PopupInfoResponse;
import com.lgcns.response.SliceResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PopupServiceImpl implements PopupService {

    private final ManagerServiceClient managerServiceClient;
    private final ReservationServiceClient reservationServiceClient;

    @Override
    public SliceResponse<PopupInfoResponse> findPopupsByName(
            String keyword, Long lastPopupId, int size) {
        return managerServiceClient.findPopupsByName(keyword, lastPopupId, size);
    }

    @Override
    public PopupDetailsResponse findPopupDetailsById(Long popupId) {
        return managerServiceClient.findPopupDetailsById(popupId);
    }

    @Override
    public List<PopupInfoResponse> findHotPopups() {
        List<Long> popupIds = reservationServiceClient.findHotPopupIds();
        PopupIdsRequest popupIdsRequest = new PopupIdsRequest(popupIds);
        return managerServiceClient.findHotPopupsByIds(popupIdsRequest);
    }
}
