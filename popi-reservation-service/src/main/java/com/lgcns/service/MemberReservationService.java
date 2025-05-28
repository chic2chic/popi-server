package com.lgcns.service;

import com.lgcns.dto.response.AvailableDateResponse;
import com.lgcns.dto.response.ReservationDetailResponse;
import com.lgcns.dto.response.SurveyChoiceResponse;
import java.util.List;

public interface MemberReservationService {
    AvailableDateResponse findAvailableDate(String memberId, Long popupId, String date);

    List<SurveyChoiceResponse> findSurveyChoicesByPopupId(String memberId, Long popupId);

    List<ReservationDetailResponse> findReservationInfo(String memberId);

    List<Long> findHotPopupIds();
}
