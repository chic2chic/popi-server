package com.lgcns.service;

import com.lgcns.dto.response.PopupDetailsResponse;
import com.lgcns.dto.response.PopupInfoResponse;
import com.lgcns.response.SliceResponse;
import java.util.List;

public interface PopupService {
    SliceResponse<PopupInfoResponse> findPopupsByName(String keyword, Long lastPopupId, int size);

    PopupDetailsResponse findPopupDetailsById(Long popupId);

    List<PopupInfoResponse> findHotPopups();
}
