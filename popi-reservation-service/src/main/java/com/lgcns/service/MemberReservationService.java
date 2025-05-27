package com.lgcns.service;

import com.lgcns.dto.response.AvailableDateResponse;
import com.lgcns.dto.response.ReservationInfoResponse;
import com.lgcns.dto.response.SurveyChoiceResponse;
import java.util.List;

public interface MemberReservationService {
    AvailableDateResponse findAvailableDate(String memberId, Long popupId, String date);

    List<SurveyChoiceResponse> findSurveyChoicesByPopupId(String memberId, Long popupId);

    List<ReservationInfoResponse> findReservationInfo(String memberId);
}
