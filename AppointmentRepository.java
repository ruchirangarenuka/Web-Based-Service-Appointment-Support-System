package com.example.sas.repository;

import com.example.sas.entity.Appointment;
import com.example.sas.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByCustomer(User customer);
    List<Appointment> findByCustomerOrderByCreatedAtDesc(User customer);
    List<Appointment> findByStatus(Appointment.Status status);
    List<Appointment> findByAppointmentDate(LocalDate date);
    List<Appointment> findByAppointmentDateBetween(LocalDate start, LocalDate end);
    long countByStatus(Appointment.Status status);

    @Query("SELECT a FROM Appointment a ORDER BY a.createdAt DESC")
    List<Appointment> findAllOrderByCreatedAtDesc();
}
