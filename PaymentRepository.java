package com.example.sas.repository;

import com.example.sas.entity.Payment;
import com.example.sas.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByCustomer(User customer);
    List<Payment> findByCustomerOrderByCreatedAtDesc(User customer);
    Optional<Payment> findByAppointmentId(Long appointmentId);
    List<Payment> findByStatus(Payment.Status status);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'PAID'")
    BigDecimal getTotalRevenue();
}
