package com.lgcns.repository;

import com.lgcns.domain.MemberReservation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberReservationRepository
        extends JpaRepository<MemberReservation, Long>, MemberReservationRepositoryCustom {
    List<MemberReservation> findByMemberId(Long memberId);

    Boolean existsMemberReservationByMemberIdAndReservationId(Long memberId, Long reservationId);
}
