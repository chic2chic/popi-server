package com.lgcns.controller;

import com.lgcns.dto.response.AvailableDateResponse;
import com.lgcns.service.MemberReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
}
