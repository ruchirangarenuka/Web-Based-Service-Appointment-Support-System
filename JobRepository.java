package com.example.sas.repository;

import com.example.sas.entity.Job;
import com.example.sas.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByAssignedStaff(User staff);
    List<Job> findByAssignedStaffOrderByCreatedAtDesc(User staff);
    List<Job> findByStatus(Job.Status status);
    Optional<Job> findByAppointmentId(Long appointmentId);
    long countByStatus(Job.Status status);
    long countByAssignedStaff(User staff);
}
