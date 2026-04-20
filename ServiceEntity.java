package com.example.sas.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "services")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    private Integer durationMinutes;

    private String category;

    private String imageUrl;

    private boolean active = true;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL)
    private List<Appointment> appointments;

    public String getFormattedDuration() {
        if (durationMinutes == null || durationMinutes == 0) return "N/A";
        int hrs = durationMinutes / 60;
        int mins = durationMinutes % 60;
        
        StringBuilder sb = new StringBuilder();
        if (hrs > 0) {
            sb.append(hrs).append(hrs == 1 ? "hr " : "hrs ");
        }
        if (mins > 0 || hrs == 0) {
            sb.append(mins).append(" minutes");
        }
        return sb.toString().trim();
    }
}
