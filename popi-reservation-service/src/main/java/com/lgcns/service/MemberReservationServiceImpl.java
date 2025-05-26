package com.lgcns.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lgcns.client.managerClient.ManagerServiceClient;
import com.lgcns.client.managerClient.dto.response.DailyReservation;
import com.lgcns.client.managerClient.dto.response.MonthlyReservationResponse;
import com.lgcns.client.managerClient.dto.response.TimeSlot;
import com.lgcns.client.memberClient.MemberServiceClient;
import com.lgcns.domain.MemberReservation;
import com.lgcns.dto.response.*;
import com.lgcns.error.exception.CustomException;
import com.lgcns.event.dto.MemberReservationUpdateEvent;
import com.lgcns.exception.MemberReservationErrorCode;
import com.lgcns.repository.MemberReservationRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberReservationServiceImpl implements MemberReservationService {
    private final MemberReservationRepository memberReservationRepository;

    private final ManagerServiceClient managerServiceClient;
    private final MemberServiceClient memberServiceClient;

    private final ApplicationEventPublisher eventPublisher;
    private final RedisTemplate<String, Long> redisTemplate;

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

    @Override
    public void createMemberReservation(String memberId, Long reservationId) {
        Long memberIdLong = validateMemberId(Long.parseLong(memberId));
        validateMemberReservationExists(memberIdLong, reservationId);

        Long possibleCount = redisTemplate.opsForValue().decrement(reservationId.toString());
        if (possibleCount == null || possibleCount < 0) {
            safeIncrement(reservationId.toString());
            throw new CustomException(MemberReservationErrorCode.RESERVATION_FAILED);
        }

        MemberReservation memberReservation;
        try {
            memberReservation =
                    MemberReservation.createMemberReservation(
                            reservationId, memberIdLong, null, null, null, null);
            memberReservationRepository.save(memberReservation);
        } catch (Exception e) {
            safeIncrement(reservationId.toString());
            throw new CustomException(MemberReservationErrorCode.RESERVATION_FAILED);
        }

        eventPublisher.publishEvent(MemberReservationUpdateEvent.of(memberReservation.getId(), 0L));
    }

    private Long validateMemberId(Long memberId) {
        MemberInternalInfoResponse memberInfo = memberServiceClient.findMemberInfo(memberId);
        return memberInfo.memberId();
    }

    private void validateMemberReservationExists(Long memberId, Long reservationId) {
        if (memberReservationRepository.existsMemberReservationByMemberIdAndReservationId(
                memberId, reservationId)) {
            throw new CustomException(MemberReservationErrorCode.RESERVATION_ALREADY_EXISTS);
        }
    }

    private void validateYearMonthFormat(String date) {
        try {
            YearMonth.parse(date);
        } catch (DateTimeParseException e) {
            throw new CustomException(MemberReservationErrorCode.INVALID_DATE_FORMAT);
        }
    }

    @CircuitBreaker(name = "redisCircuitBreaker", fallbackMethod = "handleRedisFailure")
    @Retry(name = "redisRetry")
    private void safeIncrement(String key) {
        redisTemplate.opsForValue().increment(key);
    }

    private void handleRedisFailure(String key, Throwable t) {
        log.error(
                "[예약 인원 복구 실패] Redis increment 실패 - key: {}, message: {}", key, t.getMessage(), t);
    }

    private String createMemberReservationImageString(
            Long id,
            Long reservationId,
            Long memberId,
            Long popupId,
            LocalDate reservationDate,
            LocalTime reservationTime) {

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("id", id);
            data.put("reservationId", reservationId);
            data.put("memberId", memberId);
            data.put("popupId", popupId);
            data.put("reservationDate", reservationDate.toString());
            data.put("reservationTime", reservationTime.toString());

            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(data);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(json, BarcodeFormat.QR_CODE, 300, 300);
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", outputStream);
            byte[] imageBytes = outputStream.toByteArray();

            return Base64.getEncoder().encodeToString(imageBytes);

        } catch (WriterException | IOException e) {
            throw new CustomException(MemberReservationErrorCode.QR_CODE_GENERATION_FAILED);
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
