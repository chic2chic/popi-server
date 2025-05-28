package com.lgcns.externalApi;

import com.lgcns.dto.response.AvailableDateResponse;
import com.lgcns.dto.response.ReservationDetailResponse;
import com.lgcns.dto.response.SurveyChoiceResponse;
import com.lgcns.service.MemberReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @GetMapping("/{popupId}")
    @Operation(summary = "가능한 예약 날짜 조회", description = "특정 연월에 대한 예약 가능 날짜를 조회합니다.")
    public AvailableDateResponse availableDateFind(
            @RequestHeader("member-id") String memberId,
            @PathVariable Long popupId,
            @RequestParam String date) {
        return memberReservationService.findAvailableDate(memberId, popupId, date);
    }

    @GetMapping("/popups/{popupId}/survey")
    @Operation(summary = "설문지 조회", description = "해당 팝업에 대한 설문지 선지들을 조회합니다.")
    public List<SurveyChoiceResponse> choiceListByPopupIdFind(
            @RequestHeader("member-id") String memberId, @PathVariable Long popupId) {
        return memberReservationService.findSurveyChoicesByPopupId(memberId, popupId);
    }

    @PostMapping("/{reservationId}")
    @Operation(summary = "회원 예약 생성", description = "예약을 생성합니다. 예약 ID를 사용하여 예약을 생성합니다.")
    public ResponseEntity<Void> createMemberReservation(
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
}
