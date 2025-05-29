package com.lgcns.repository;

import com.lgcns.domain.MemberReservation;
import com.lgcns.dto.response.DailyReservationCountResponse;
import java.time.LocalDate;
import java.util.List;

public interface MemberReservationRepositoryCustom {

    List<DailyReservationCountResponse> findDailyReservationCount(
            Long popupId, LocalDate popupOpenDate, LocalDate popupCloseDate, String date);

    MemberReservation findUpcomingReservation(Long memberId);

    List<Long> findHotPopupIds();
}
