package com.lgcns.service;

import com.lgcns.dto.request.SurveyChoiceRequest;
import com.lgcns.dto.request.QrEntranceInfoRequest;
import com.lgcns.dto.response.AvailableDateResponse;
import com.lgcns.dto.response.DailyMemberReservationCountResponse;
import com.lgcns.dto.response.ReservationDetailResponse;
import com.lgcns.dto.response.SurveyChoiceResponse;
import java.util.List;

public interface MemberReservationService {
    AvailableDateResponse findAvailableDate(Long popupId, String date);

    List<SurveyChoiceResponse> findSurveyChoicesByPopupId(Long popupId);

    List<ReservationDetailResponse> findReservationInfo(String memberId);

    ReservationDetailResponse findUpcomingReservationInfo(String memberId);

    void createMemberReservation(String memberId, Long reservationId);

    void updateMemberReservation(Long memberReservationId);

    void cancelMemberReservation(Long memberReservationId);

    List<Long> findHotPopupIds();

    DailyMemberReservationCountResponse findDailyMemberReservationCount(Long popupId);

    void createMemberAnswer(Long popupId, String memberId, List<SurveyChoiceRequest> surveyChoices);

    void isEnterancePossible(QrEntranceInfoRequest qrEntranceInfoRequest, Long popupId);
}
