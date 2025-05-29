package com.lgcns.repository;

import com.lgcns.domain.MemberReservation;
import com.lgcns.domain.MemberReservationStatus;
import com.lgcns.dto.response.DailyMemberReservationCountResponse;
import com.lgcns.dto.response.DailyReservationCountResponse;
import java.time.LocalDate;
import java.util.List;

public interface MemberReservationRepositoryCustom {

    List<DailyReservationCountResponse> findDailyReservationCount(
            Long popupId, LocalDate popupOpenDate, LocalDate popupCloseDate, String date);

    MemberReservation findUpcomingReservation(Long memberId, MemberReservationStatus status);

    List<Long> findHotPopupIds();

    List<MemberReservation> findByMemberIdAndStatus(Long memberId, MemberReservationStatus status);

    DailyMemberReservationCountResponse findDailyMemberReservationCount(
            Long popupId, LocalDate today);
}
