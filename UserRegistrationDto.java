package com.example.sas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalTime;

@Data
public class UserRegistrationDto {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(?:\\+94|0)[0-9]{9}$", message = "Invalid phone number. Use a valid Sri Lankan number (e.g. 0771234567 or +94771234567)")
    private String phone;

    private String role = "CUSTOMER"; // default

    // ===== CUSTOMER SPECIFIC FIELDS =====
    private String address;
    private String city;

    // ===== STAFF/WORKER SPECIFIC FIELDS =====
    private MultipartFile idPhoto;
    private String serviceType;
    private String[] availability;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime availableFromTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime availableToTime;

    private Integer yearsOfExperience;
    private String qualifications;
    private String emergencyContactName;

    @Pattern(regexp = "^(?:\\+94|0)[0-9]{9}$|^$", message = "Emergency contact phone must be a valid Sri Lankan number")
    private String emergencyContactPhone;

    private Double hourlyRate;

    // ===== ADMIN SPECIFIC FIELDS =====
    private String department;
    private String employeeId;
    private String securityQuestion;
    private String securityAnswer;
    private String accessLevel;

    // Keep explicit accessors for role so worker-only registration works even if Lombok indexing is unavailable in the IDE.
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}