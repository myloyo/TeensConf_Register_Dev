package com.teensconf.repository;

import com.teensconf.entity.PaymentReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt, Long> {
    List<PaymentReceipt> findByRegistrationId(Long registrationId);
    Optional<PaymentReceipt> findByPaymentReference(String paymentReference);
    boolean existsByPaymentReference(String paymentReference);
}