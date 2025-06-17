package com.lgcns.service;

import static com.lgcns.domain.MemberReservationStatus.RESERVED;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lgcns.client.managerClient.ManagerServiceClient;
import com.lgcns.client.managerClient.dto.request.PopupIdsRequest;
import com.lgcns.client.managerClient.dto.response.*;
import com.lgcns.client.memberClient.MemberGrpcClient;
import com.lgcns.client.memberClient.MemberServiceClient;
import com.lgcns.domain.MemberReservation;
import com.lgcns.dto.request.QrEntranceInfoRequest;
import com.lgcns.dto.request.SurveyChoiceRequest;
import com.lgcns.dto.response.*;
import com.lgcns.error.exception.CustomException;
import com.lgcns.error.exception.GlobalErrorCode;
import com.lgcns.event.dto.MemberReservationNotificationEvent;
import com.lgcns.event.dto.MemberReservationUpdateEvent;
import com.lgcns.exception.MemberReservationErrorCode;
import com.lgcns.kafka.message.MemberAnswerMessage;
import com.lgcns.kafka.message.MemberEnteredMessage;
import com.lgcns.kafka.producer.MemberAnswerProducer;
import com.lgcns.repository.MemberReservationRepository;
import com.popi.common.grpc.member.MemberInternalIdRequest;
import com.popi.common.grpc.member.MemberInternalInfoResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private final MemberGrpcClient memberGrpcClient;

    private final ApplicationEventPublisher eventPublisher;

    @Qualifier("reservationRedisTemplate")
    private final RedisTemplate<String, Long> reservationRedisTemplate;

    @Qualifier("notificationRedisTemplate")
    private final RedisTemplate<String, String> notificationRedisTemplate;

    private final MemberAnswerProducer memberAnswerProducer;

    private static final int DEFAULT_SURVEY_COUNT = 4;

    @Override
    public AvailableDateResponse findAvailableDate(Long popupId, String date) {

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
    public List<SurveyChoiceResponse> findSurveyChoicesByPopupId(Long popupId) {
        return managerServiceClient.findSurveyChoicesByPopupId(popupId);
    }

    @Override
    public void createMemberAnswer(
            Long popupId, String memberId, List<SurveyChoiceRequest> surveyChoices) {

        if (surveyChoices.size() != DEFAULT_SURVEY_COUNT) {
            throw new CustomException(MemberReservationErrorCode.INVALID_SURVEY_CHOICES_COUNT);
        }

        memberAnswerProducer.sendMessage(
                MemberAnswerMessage.of(Long.parseLong(memberId), surveyChoices));

        // TODO 상품 서비스로 메시지 발행 후 취향 저격 상품 생성 로직 추가
    }

    @Override
    public List<ReservationDetailResponse> findReservationInfo(String memberId) {
        List<MemberReservation> memberReservationList =
                memberReservationRepository.findByMemberIdAndStatus(
                        Long.parseLong(memberId), RESERVED);

        if (memberReservationList.isEmpty()) {
            return List.of();
        }

        List<Long> popupIds =
                memberReservationList.stream()
                        .map(MemberReservation::getPopupId)
                        .distinct()
                        .toList();

        List<ReservationPopupInfoResponse> reservationPopupInfoList =
                managerServiceClient.findReservedPopupInfoList(PopupIdsRequest.of(popupIds));

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
    public ReservationDetailResponse findUpcomingReservationInfo(String memberId) {

        MemberReservation upcomingReservation =
                memberReservationRepository.findUpcomingReservation(Long.parseLong(memberId));

        if (upcomingReservation == null) {
            return null;
        }

        ReservationPopupInfoResponse reservationPopupInfo =
                managerServiceClient.findReservedPopupInfo(upcomingReservation.getPopupId());

        return ReservationDetailResponse.of(upcomingReservation, reservationPopupInfo);
    }

    @Override
    public void isEntrancePossible(QrEntranceInfoRequest qrEntranceInfoRequest, Long popupId) {
        LocalDate currentDate = LocalDate.now();
        LocalTime currentTime = LocalTime.now();

        MemberReservation memberReservation =
                memberReservationRepository
                        .findById(qrEntranceInfoRequest.memberReservationId())
                        .orElseThrow(
                                () ->
                                        new CustomException(
                                                MemberReservationErrorCode
                                                        .MEMBER_RESERVATION_NOT_FOUND));

        if (memberReservation.getIsEntered()) {
            throw new CustomException(MemberReservationErrorCode.RESERVATION_ALREADY_ENTERED);
        }

        if (!memberReservation.getPopupId().equals(popupId)) {
            throw new CustomException(MemberReservationErrorCode.RESERVATION_POPUP_MISMATCH);
        }

        if (!memberReservation.getReservationId().equals(qrEntranceInfoRequest.reservationId())) {
            throw new CustomException(MemberReservationErrorCode.INVALID_QR_CODE);
        }

        if (!memberReservation.getReservationDate().equals(currentDate)) {
            throw new CustomException(MemberReservationErrorCode.RESERVATION_DATE_MISMATCH);
        }

        if (currentTime.isBefore(memberReservation.getReservationTime())) {
            throw new CustomException(MemberReservationErrorCode.RESERVATION_TIME_MISMATCH);
        }

        if (currentTime.isAfter(memberReservation.getReservationTime().plusMinutes(31))) {
            throw new CustomException(MemberReservationErrorCode.RESERVATION_TIME_MISMATCH);
        }

        memberReservation.updateIsEntered();
        eventPublisher.publishEvent(MemberEnteredMessage.from(qrEntranceInfoRequest));
    }

    @Override
    public void createMemberReservation(String memberId, Long reservationId) {
        validateMemberReservationExists(Long.parseLong(memberId), reservationId);

        Long possibleCount =
                reservationRedisTemplate.opsForValue().decrement(reservationId.toString());
        if (possibleCount == null || possibleCount < 0) {
            safeIncrement(reservationId.toString());
            throw new CustomException(MemberReservationErrorCode.RESERVATION_FAILED);
        }

        MemberReservation memberReservation;
        try {
            memberReservation =
                    MemberReservation.createMemberReservation(
                            reservationId, Long.parseLong(memberId));
            memberReservationRepository.save(memberReservation);
        } catch (Exception e) {
            safeIncrement(reservationId.toString());
            throw new CustomException(MemberReservationErrorCode.RESERVATION_FAILED);
        }

        eventPublisher.publishEvent(MemberReservationUpdateEvent.of(memberReservation.getId(), 0L));
    }

    @Override
    public void updateMemberReservation(Long memberReservationId) {
        MemberReservation memberReservation = findMemberReservationById(memberReservationId);

        MemberInternalInfoResponse memberInfo =
                memberGrpcClient.findByMemberId(
                        MemberInternalIdRequest.newBuilder()
                                .setMemberId(memberReservation.getMemberId())
                                .build());

        ReservationInfoResponse reservationInfoResponse =
                managerServiceClient.findReservationById(memberReservation.getReservationId());

        String imageByte =
                createMemberReservationImageString(
                        memberReservation.getId(),
                        memberReservation.getReservationId(),
                        reservationInfoResponse.popupId(),
                        memberInfo.getAge().toString(),
                        memberInfo.getGender().toString(),
                        reservationInfoResponse.reservationDate(),
                        reservationInfoResponse.reservationTime());

        memberReservation.updateMemberReservation(
                reservationInfoResponse.popupId(),
                imageByte,
                reservationInfoResponse.reservationDate(),
                reservationInfoResponse.reservationTime());

        eventPublisher.publishEvent(MemberReservationNotificationEvent.from(memberReservation));
    }

    @Override
    public void createReservationNotification(MemberReservationNotificationEvent event) {
        try {
            LocalDate reservationDate = event.reservationDate();
            LocalTime reservationTime = event.reservationTime();
            LocalDateTime reservationDateTime = LocalDateTime.of(reservationDate, reservationTime);

            long epochTime =
                    reservationDateTime
                            .minusHours(1)
                            .atZone(ZoneId.of("Asia/Seoul"))
                            .toEpochSecond();

            String memberKey = event.memberReservationId() + "|" + event.memberId();

            notificationRedisTemplate
                    .opsForZSet()
                    .add("reservation:notifications", memberKey, epochTime);

        } catch (Exception e) {
            log.error("예약 알림 등록 오류: {}", e.getMessage(), e);
        }
    }

    @Override
    public void cancelMemberReservation(Long memberReservationId) {
        MemberReservation memberReservation = findMemberReservationById(memberReservationId);
        Long reservationId = memberReservation.getReservationId();

        if (reservationId == null)
            throw new CustomException(MemberReservationErrorCode.RESERVATION_NOT_FOUND);

        memberReservationRepository.delete(memberReservation);
        safeIncrement(reservationId.toString());

        String memberKey =
                memberReservation.getReservationId() + "|" + memberReservation.getMemberId();

        notificationRedisTemplate.opsForZSet().remove("reservation:notifications", memberKey);
    }

    @Override
    public Map<Long, DayOfWeekReservationStatsResponse> getAllDayOfWeekReservationStats() {
        try {
            List<DayOfWeekReservationStatsResponse> allStats =
                    memberReservationRepository.findAllDayOfWeekReservationStats();

            log.info("요일별 예약자 수 조회 완료. 조회된 팝업 수 : {}", allStats.size());

            return allStats.stream()
                    .collect(
                            Collectors.toMap(
                                    DayOfWeekReservationStatsResponse::popupId, stats -> stats));
        } catch (Exception e) {
            log.error("DB 조회 및 쿼리 수행 중 에러 발생 {}", e.getMessage(), e);
            throw new CustomException(GlobalErrorCode.DATABASE_ERROR);
        }
    }

    private MemberReservation findMemberReservationById(Long memberReservationId) {
        return memberReservationRepository
                .findById(memberReservationId)
                .orElseThrow(
                        () ->
                                new CustomException(
                                        MemberReservationErrorCode.MEMBER_RESERVATION_NOT_FOUND));
    }

    private void validateMemberReservationExists(Long memberId, Long reservationId) {
        if (memberReservationRepository.existsMemberReservationByMemberIdAndReservationId(
                memberId, reservationId)) {
            throw new CustomException(MemberReservationErrorCode.RESERVATION_ALREADY_EXISTS);
        }
    }

    @Transactional(readOnly = true)
    public DailyMemberReservationCountResponse findDailyMemberReservationCount(Long popupId) {
        LocalDate today = LocalDate.now();
        return memberReservationRepository.findDailyMemberReservationCount(popupId, today);
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
        reservationRedisTemplate.opsForValue().increment(key);
    }

    private void handleRedisFailure(String key, Throwable t) {
        log.error(
                "[예약 인원 복구 실패] Redis increment 실패 - key: {}, message: {}", key, t.getMessage(), t);
    }

    private String createMemberReservationImageString(
            Long memberReservationId,
            Long reservationId,
            Long popupId,
            String age,
            String gender,
            LocalDate reservationDate,
            LocalTime reservationTime) {

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("memberReservationId", memberReservationId);
            data.put("reservationId", reservationId);
            data.put("popupId", popupId);
            data.put("age", age);
            data.put("gender", gender);
            data.put("reservationDate", reservationDate.toString());
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            data.put("reservationTime", reservationTime.format(timeFormatter));

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
