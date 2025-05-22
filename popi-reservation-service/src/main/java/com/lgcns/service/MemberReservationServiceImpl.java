package com.lgcns.service;

import com.lgcns.client.ManagerServiceClient;
import com.lgcns.client.dto.DailyReservation;
import com.lgcns.client.dto.MonthlyReservationDto;
import com.lgcns.client.dto.TimeSlot;
import com.lgcns.dto.response.*;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.MemberReservationErrorCode;
import com.lgcns.repository.MemberReservationRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberReservationServiceImpl implements MemberReservationService {

    private final ManagerServiceClient managerServiceClient;
    private final MemberReservationRepository memberReservationRepository;

    @Override
    public AvailableDateResponse findAvailableDate(String memberId, Long popupId, String date) {

        validateYearMonthFormat(date);

        MonthlyReservationDto monthlyReservation =
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
            MonthlyReservationDto monthlyReservation,
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
