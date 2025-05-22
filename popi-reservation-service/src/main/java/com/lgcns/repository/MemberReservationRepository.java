package com.lgcns.repository;

import com.lgcns.domain.MemberReservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberReservationRepository
        extends JpaRepository<MemberReservation, Long>, MemberReservationRepositoryCustom {}
