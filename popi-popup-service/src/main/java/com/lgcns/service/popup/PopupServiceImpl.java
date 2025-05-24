package com.lgcns.service.popup;

import com.lgcns.client.ManagerServiceClient;
import com.lgcns.dto.popup.response.PopupInfoResponse;
import com.lgcns.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PopupServiceImpl implements PopupService {
    private final ManagerServiceClient managerServiceClient;

    @Override
    public SliceResponse<PopupInfoResponse> findAllPopups(Long lastPopupId, int size) {
        return managerServiceClient.findAllPopups(lastPopupId, size);
    }
}
