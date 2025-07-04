package com.lgcns.internalApi;

import com.lgcns.dto.response.DailyMemberReservationCountResponse;
import com.lgcns.dto.response.DayOfWeekReservationStatsResponse;
import com.lgcns.service.MemberReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Tag(name = "예약 서버 Internal API", description = "예약 서버 Internal API입니다.")
public class MemberReservationInternalController {

    private final MemberReservationService memberReservationService;

    @GetMapping("/popups/popularity")
    public List<Long> findHotPopups() {
        return memberReservationService.findHotPopupIds();
    }

    @GetMapping("/{popupId}/daily-count")
    @Operation(summary = "오늘 예약자 수 조회", description = "오늘 날짜에 해당하는 예약자 수를 조회합니다.")
    public DailyMemberReservationCountResponse findDailyMemberReservationCount(
            @PathVariable(name = "popupId") Long popupId) {
        return memberReservationService.findDailyMemberReservationCount(popupId);
    }

    @GetMapping("/day-of-week-count")
    public Map<Long, DayOfWeekReservationStatsResponse> getAllDayOfWeekReservationStats() {
        return memberReservationService.getAllDayOfWeekReservationStats();
    }
}
