package com.lgcns.service;

import com.lgcns.client.ManagerServiceClient;
import com.lgcns.dto.response.PopupDetailsResponse;
import com.lgcns.dto.response.PopupInfoResponse;
import com.lgcns.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PopupServiceImpl implements PopupService {

    private final ManagerServiceClient managerServiceClient;

    @Override
    public SliceResponse<PopupInfoResponse> findPopupsByName(
            String keyword, Long lastPopupId, int size) {
        return managerServiceClient.findPopupsByName(keyword, lastPopupId, size);
    }

    @Override
    public PopupDetailsResponse findPopupDetailsById(Long popupId) {
        return managerServiceClient.findPopupDetailsById(popupId);
    }
}
