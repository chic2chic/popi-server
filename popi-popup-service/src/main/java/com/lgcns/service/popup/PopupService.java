package com.lgcns.service.popup;

import com.lgcns.dto.popup.response.PopupInfoResponse;
import com.lgcns.response.SliceResponse;

public interface PopupService {
    SliceResponse<PopupInfoResponse> findPopupsByName(
            String searchName, Long lastPopupId, int size);
}
