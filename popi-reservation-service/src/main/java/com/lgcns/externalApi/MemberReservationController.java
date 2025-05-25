package com.lgcns.externalApi;

import com.lgcns.dto.response.AvailableDateResponse;
import com.lgcns.service.MemberReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.lgcns.dto.response.SurveyChoiceResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "예약 서버 API", description = "예약 서버 API입니다.")
public class MemberReservationController {

    private final MemberReservationService memberReservationService;

    @GetMapping("/popups/{popupId}")
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
}
