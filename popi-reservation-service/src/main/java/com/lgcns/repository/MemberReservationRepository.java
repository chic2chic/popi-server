package com.lgcns.repository;

import com.lgcns.domain.MemberReservation;
import com.lgcns.dto.response.DayOfWeekReservationStatsResponse;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MemberReservationRepository
        extends JpaRepository<MemberReservation, Long>, MemberReservationRepositoryCustom {
    List<MemberReservation> findByMemberId(Long memberId);

    Boolean existsMemberReservationByMemberIdAndReservationId(Long memberId, Long reservationId);

    @Query(
            """
        SELECT new com.lgcns.dto.response.DayOfWeekReservationStatsResponse(
            mr.popupId,
            CAST(SUM(CASE WHEN FUNCTION('DAYOFWEEK', mr.reservationDate) = 2 THEN 1 ELSE 0 END) AS int),
            CAST(SUM(CASE WHEN FUNCTION('DAYOFWEEK', mr.reservationDate) = 3 THEN 1 ELSE 0 END) AS int),
            CAST(SUM(CASE WHEN FUNCTION('DAYOFWEEK', mr.reservationDate) = 4 THEN 1 ELSE 0 END) AS int),
            CAST(SUM(CASE WHEN FUNCTION('DAYOFWEEK', mr.reservationDate) = 5 THEN 1 ELSE 0 END) AS int),
            CAST(SUM(CASE WHEN FUNCTION('DAYOFWEEK', mr.reservationDate) = 6 THEN 1 ELSE 0 END) AS int),
            CAST(SUM(CASE WHEN FUNCTION('DAYOFWEEK', mr.reservationDate) = 7 THEN 1 ELSE 0 END) AS int),
            CAST(SUM(CASE WHEN FUNCTION('DAYOFWEEK', mr.reservationDate) = 1 THEN 1 ELSE 0 END) AS int)
        )
        FROM MemberReservation mr
        WHERE mr.popupId IS NOT NULL
            AND mr.status = com.lgcns.domain.MemberReservationStatus.RESERVED
        GROUP BY mr.popupId
        """)
    List<DayOfWeekReservationStatsResponse> findDayOfWeekReservationStats();
}
