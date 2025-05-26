package com.lgcns.service;

import com.lgcns.dto.response.PopupDetailsResponse;
import com.lgcns.dto.response.PopupInfoResponse;
import com.lgcns.response.SliceResponse;

public interface PopupService {
    SliceResponse<PopupInfoResponse> findPopupsByName(
            String searchName, Long lastPopupId, int size);

    PopupDetailsResponse findPopupDetailsById(Long popupId);
}
