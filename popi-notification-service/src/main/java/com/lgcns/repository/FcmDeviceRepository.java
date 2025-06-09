package com.lgcns.repository;

import com.lgcns.domain.FcmDevice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FcmDeviceRepository
        extends JpaRepository<FcmDevice, Long>, FcmDeviceRepositoryCustom {}
