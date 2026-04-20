package com.example.sas.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Column(name = "commission_amount", precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "staff_payout", precision = 12, scale = 2)
    private BigDecimal staffPayout;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 30,
            columnDefinition = "VARCHAR(30)")
    private Method method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30,
            columnDefinition = "VARCHAR(30)")
    private Status status = Status.PENDING;

    @Column(length = 50)
    private String transactionId;

    @Column(length = 255)
    private String receiptImagePath;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean adminConfirmed = false;

    @Column(length = 50)
    private String invoiceNumber;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime paidAt;

    public enum Method {
        CARD,
        BANK_TRANSFER,
        CASH
    }

    public enum Status {
        PENDING,
        RECEIPT_UPLOADED,
        CONFIRMED,
        PAID,
        REFUNDED,
        FAILED
    }
}
