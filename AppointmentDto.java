package com.example.sas.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AppointmentDto {

    @NotNull(message = "Service is required")
    private Long serviceId;

    @NotNull(message = "Date is required")
    @FutureOrPresent(message = "Appointment date cannot be in the past")
    private LocalDate appointmentDate;

    @NotNull(message = "Time is required")
    private LocalTime appointmentTime;

    private String notes;

    private String address;

    private Integer numberOfWorkers;
}
