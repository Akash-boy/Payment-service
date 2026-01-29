package com.example.repository;

import com.example.entities.Payment;
import com.example.entities.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    // Find all payments for a user with pagination
    Page<Payment> findByUserId(Long userId, Pageable pageable);

    // Find by idempotency key to prevent duplicate payments
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    // Check if payment exists for idempotency key with lock for thread safety
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.idempotencyKey = :idempotencyKey")
    Optional<Payment> findByIdempotencyKeyWithLock(@Param("idempotencyKey") String idempotencyKey);

    // Find payments by status
    List<Payment> findByStatus(PaymentStatus status);

    // Find payments by user and status
    Page<Payment> findByUserIdAndStatus(Long userId, PaymentStatus status, Pageable pageable);

    // Find failed payments for retry
    @Query("SELECT p FROM Payment p WHERE p.status = 'FAILED' AND p.retryCount < :maxRetries")
    List<Payment> findFailedPaymentsForRetry(@Param("maxRetries") int maxRetries);
}