package com.lgcns.externalApi;

import com.lgcns.dto.request.QrEntranceInfoRequest;
import com.lgcns.dto.request.SurveyChoiceRequest;
import com.lgcns.dto.response.AvailableDateResponse;
import com.lgcns.dto.response.ReservationDetailResponse;
import com.lgcns.dto.response.SurveyChoiceResponse;
import com.lgcns.service.MemberReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "예약 서버 API", description = "예약 서버 API입니다.")
public class MemberReservationController {

    private final MemberReservationService memberReservationService;

    @GetMapping("/popups/{popupId}")
    @Operation(summary = "가능한 예약 날짜 조회", description = "특정 연월에 대한 예약 가능 날짜를 조회합니다.")
    public AvailableDateResponse availableDateFind(
            @PathVariable Long popupId, @RequestParam String date) {
        return memberReservationService.findAvailableDate(popupId, date);
    }

    @GetMapping("/popups/{popupId}/survey")
    @Operation(summary = "설문지 조회", description = "해당 팝업에 대한 설문지 선지들을 조회합니다.")
    public List<SurveyChoiceResponse> choiceListByPopupIdFind(@PathVariable Long popupId) {
        return memberReservationService.findSurveyChoicesByPopupId(popupId);
    }

    @PostMapping("/popups/{popupId}/survey")
    @Operation(summary = "설문지 응답 저장", description = "해당 팝업에 대한 설문지 응답을 저장합니다.")
    public ResponseEntity<Void> memberAnswerCreate(
            @PathVariable Long popupId,
            @Valid @RequestBody List<SurveyChoiceRequest> surveyChoices) {
        memberReservationService.createMemberAnswer(popupId, surveyChoices);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{reservationId}")
    @Operation(summary = "회원 예약 생성", description = "예약을 생성합니다. 예약 ID를 사용하여 예약을 생성합니다.")
    public ResponseEntity<Void> memberReservationCreate(
            @RequestHeader("member-id") String memberId, @PathVariable Long reservationId) {
        memberReservationService.createMemberReservation(memberId, reservationId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    @Operation(summary = "내 예약 목록 조회", description = "사용자의 예약 목록을 조회합니다.")
    public List<ReservationDetailResponse> reservationInfoFind(
            @RequestHeader("member-id") String memberId) {
        return memberReservationService.findReservationInfo(memberId);
    }

    @GetMapping("/upcoming")
    @Operation(summary = "가까운 팝업 조회", description = "사용자가 예약한 팝업 중, 가장 가까운 날짜의 팝업을 조회합니다.")
    public ReservationDetailResponse upcomingReservationInfoFind(
            @RequestHeader("member-id") String memberId) {
        return memberReservationService.findUpcomingReservationInfo(memberId);
    }

    @DeleteMapping("/{memberReservationId}")
    @Operation(summary = "회원 예약 취소", description = "예약 ID를 사용하여 회원의 예약을 취소합니다.")
    public ResponseEntity<Void> memberReservationCancel(
            @PathVariable("memberReservationId") Long memberReservationId) {
        memberReservationService.cancelMemberReservation(memberReservationId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/entrance")
    @Operation(summary = "예약 회원 입장", description = "예약 ID를 사용하여 회원의 예약을 입장 처리합니다.")
    public ResponseEntity<Void> memberReservationEntrance(
            @Valid @RequestBody QrEntranceInfoRequest qrEntranceInfoRequest,
            @RequestParam Long popupId) {
        memberReservationService.isEnterancePossible(qrEntranceInfoRequest, popupId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
