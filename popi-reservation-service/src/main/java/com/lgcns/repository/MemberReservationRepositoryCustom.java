package com.lgcns.repository;

import com.lgcns.dto.response.DailyReservationCountResponse;
import java.time.LocalDate;
import java.util.List;

public interface MemberReservationRepositoryCustom {

    List<DailyReservationCountResponse> findDailyReservationCount(
            Long popupId, LocalDate popupOpenDate, LocalDate popupCloseDate, String date);
}
