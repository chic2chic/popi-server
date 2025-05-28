package com.lgcns.service;

import com.lgcns.client.managerClient.ManagerServiceClient;
import com.lgcns.client.managerClient.dto.request.PopupIdsRequest;
import com.lgcns.client.managerClient.dto.response.DailyReservation;
import com.lgcns.client.managerClient.dto.response.MonthlyReservationResponse;
import com.lgcns.client.managerClient.dto.response.ReservationPopupInfoResponse;
import com.lgcns.client.managerClient.dto.response.TimeSlot;
import com.lgcns.domain.MemberReservation;
import com.lgcns.dto.response.*;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.MemberReservationErrorCode;
import com.lgcns.repository.MemberReservationRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberReservationServiceImpl implements MemberReservationService {

    private final ManagerServiceClient managerServiceClient;
    private final MemberReservationRepository memberReservationRepository;

    @Override
    public AvailableDateResponse findAvailableDate(String memberId, Long popupId, String date) {

        validateYearMonthFormat(date);

        MonthlyReservationResponse monthlyReservation =
                managerServiceClient.findMonthlyReservation(popupId, date);

        Map<LocalDate, Map<LocalTime, Integer>> reservationCountMap =
                buildReservationCountMap(
                        popupId,
                        monthlyReservation.popupOpenDate(),
                        monthlyReservation.popupCloseDate(),
                        date);

        List<ReservableDate> reservableDateList =
                buildReservableDateList(
                        monthlyReservation, reservationCountMap, monthlyReservation.timeCapacity());

        return AvailableDateResponse.of(
                monthlyReservation.popupOpenDate(),
                monthlyReservation.popupCloseDate(),
                reservableDateList);
    }

    @Override
    public List<SurveyChoiceResponse> findSurveyChoicesByPopupId(String memberId, Long popupId) {
        return managerServiceClient.findSurveyChoicesByPopupId(popupId);
    }

    @Override
    public List<ReservationDetailResponse> findReservationInfo(String memberId) {
        List<MemberReservation> memberReservationList =
                memberReservationRepository.findByMemberId(Long.parseLong(memberId));

        if (memberReservationList.isEmpty()) {
            return List.of();
        }

        List<Long> popupIds =
                memberReservationList.stream()
                        .map(MemberReservation::getPopupId)
                        .distinct()
                        .toList();

        List<ReservationPopupInfoResponse> reservationPopupInfoList =
                managerServiceClient.findReservedPopupInfo(PopupIdsRequest.of(popupIds));

        Map<Long, ReservationPopupInfoResponse> reservationPopupInfoMap =
                reservationPopupInfoList.stream()
                        .collect(
                                Collectors.toMap(
                                        ReservationPopupInfoResponse::popupId,
                                        popupInfo -> popupInfo));

        return memberReservationList.stream()
                .map(
                        reservation ->
                                ReservationDetailResponse.of(
                                        reservation,
                                        reservationPopupInfoMap.get(reservation.getPopupId())))
                .toList();
    }

    @Override
    public List<Long> findHotPopupIds() {
        return memberReservationRepository.findHotPopupIds();
    }

    private void validateYearMonthFormat(String date) {
        try {
            YearMonth.parse(date);
        } catch (DateTimeParseException e) {
            throw new CustomException(MemberReservationErrorCode.INVALID_DATE_FORMAT);
        }
    }

    private Map<LocalDate, Map<LocalTime, Integer>> buildReservationCountMap(
            Long popupId, LocalDate popupOpenDate, LocalDate popupCloseDate, String targetDate) {
        List<DailyReservationCountResponse> dailyReservationCountList =
                memberReservationRepository.findDailyReservationCount(
                        popupId, popupOpenDate, popupCloseDate, targetDate);

        Map<LocalDate, Map<LocalTime, Integer>> reservationCountMapByDateAndTime = new HashMap<>();

        for (DailyReservationCountResponse dailyCount : dailyReservationCountList) {
            LocalDate reservationDate = dailyCount.reservationDate();

            for (HourlyReservationCount hourlyCount : dailyCount.hourlyReservationCountList()) {
                LocalTime reservationTime = hourlyCount.reservationTime();
                int count = hourlyCount.count();

                reservationCountMapByDateAndTime
                        .computeIfAbsent(reservationDate, d -> new HashMap<>())
                        .put(reservationTime, count);
            }
        }

        return reservationCountMapByDateAndTime;
    }

    private List<ReservableDate> buildReservableDateList(
            MonthlyReservationResponse monthlyReservation,
            Map<LocalDate, Map<LocalTime, Integer>> reservationCountMap,
            int timeCapacity) {
        List<ReservableDate> reservableDateList = new ArrayList<>();

        for (DailyReservation dailyReservation : monthlyReservation.dailyReservations()) {

            LocalDate reservationDate = dailyReservation.reservationDate();
            List<ReservableTime> reservableTimeList = new ArrayList<>();
            boolean isReservableDate = false;

            for (TimeSlot timeSlot : dailyReservation.timeSlots()) {
                LocalTime time = timeSlot.time();

                int reserved =
                        reservationCountMap
                                .getOrDefault(reservationDate, Collections.emptyMap())
                                .getOrDefault(time, 0);

                boolean isReservableTime = (timeCapacity - reserved) > 0;
                if (isReservableTime) isReservableDate = true;

                reservableTimeList.add(
                        new ReservableTime(timeSlot.reservationId(), time, isReservableTime));
            }

            reservableDateList.add(
                    ReservableDate.of(reservationDate, isReservableDate, reservableTimeList));
        }

        return reservableDateList;
    }
}
