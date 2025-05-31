package com.lgcns.service;

import com.lgcns.dto.response.PopupDetailsResponse;
import com.lgcns.dto.response.PopupInfoResponse;
import com.lgcns.dto.response.PopupMapResponse;
import com.lgcns.response.SliceResponse;
import java.util.List;

public interface PopupService {
    SliceResponse<PopupInfoResponse> findPopupsByName(String keyword, Long lastPopupId, int size);

    PopupDetailsResponse findPopupDetailsById(Long popupId);

    List<PopupInfoResponse> findHotPopups();

    List<PopupMapResponse> findPopupsByMapArea(
            Double latMin, Double latMax, Double lngMin, Double lngMax);
}
