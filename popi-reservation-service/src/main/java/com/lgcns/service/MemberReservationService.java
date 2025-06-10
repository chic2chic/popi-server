package com.lgcns.service;

import com.lgcns.dto.request.QrEntranceInfoRequest;
import com.lgcns.dto.request.SurveyChoiceRequest;

import com.lgcns.dto.response.*;
import com.lgcns.event.dto.MemberReservationNotificationEvent;
import java.util.List;
import java.util.Map;

public interface MemberReservationService {
    AvailableDateResponse findAvailableDate(Long popupId, String date);

    List<SurveyChoiceResponse> findSurveyChoicesByPopupId(Long popupId);

    List<ReservationDetailResponse> findReservationInfo(String memberId);

    ReservationDetailResponse findUpcomingReservationInfo(String memberId);

    void createMemberReservation(String memberId, Long reservationId);

    void updateMemberReservation(Long memberReservationId);

    void createReservationNotification(MemberReservationNotificationEvent event);

    void cancelMemberReservation(Long memberReservationId);

    List<Long> findHotPopupIds();

    DailyMemberReservationCountResponse findDailyMemberReservationCount(Long popupId);

    void createMemberAnswer(Long popupId, String memberId, List<SurveyChoiceRequest> surveyChoices);

    void isEntrancePossible(QrEntranceInfoRequest qrEntranceInfoRequest, Long popupId);

    Map<Long, DayOfWeekReservationStatsResponse> getDayOfWeekReservationStats();
}
