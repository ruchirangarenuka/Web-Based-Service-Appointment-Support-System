package com.example.sas.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private boolean enabled = true;

    private String profileImageUrl;

    private String address;

    // NEW: ID photo URL for staff workers
    private String idPhotoUrl;

    // ===== CUSTOMER SPECIFIC FIELDS =====
    private String city;
    private String postalCode;
    private String idDocumentType;
    private String profilePictureUrl;

    // ===== STAFF SPECIFIC FIELDS =====
    // Service type the staff worker provides
    private String serviceType;

    // Stored as a comma-separated list of selected days
    private String availability;

    // Availability time window for staff
    private LocalTime availableFromTime;
    private LocalTime availableToTime;
    
    private Integer yearsOfExperience;
    private String qualifications;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private Double hourlyRate;

    // ===== ADMIN SPECIFIC FIELDS =====
    private String department;
    private String employeeId;
    private String securityQuestion;
    private String securityAnswer;
    private String accessLevel; // SUPER_ADMIN, MANAGER, STAFF_VIEWER

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<Appointment> appointments;

    @OneToMany(mappedBy = "assignedStaff", cascade = CascadeType.ALL)
    private List<Job> assignedJobs;

    public enum Role {
        ADMIN, STAFF, CUSTOMER
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
