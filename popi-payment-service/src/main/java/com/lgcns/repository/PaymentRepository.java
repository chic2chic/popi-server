package com.lgcns.repository;

import com.lgcns.domain.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long>, PaymentRepositoryCustom {
    Optional<Payment> findByMerchantUid(String merchantUid);
}
