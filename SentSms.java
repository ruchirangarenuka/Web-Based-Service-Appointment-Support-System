package com.example.sas.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sent_sms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SentSms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipientPhone;

    @Column(length = 1000, nullable = false)
    private String message;

    @Column(nullable = false)
    private String status = "SIMULATED"; // SIMULATED, SENT, FAILED

    @Column(nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    public SentSms(String recipientPhone, String message, String status) {
        this.recipientPhone = recipientPhone;
        this.message = message;
        this.status = status;
    }
}
