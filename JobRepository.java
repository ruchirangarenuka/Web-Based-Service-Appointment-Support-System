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

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(j) FROM Job j WHERE j.assignedStaff = :staff AND j.appointment.appointmentDate = :date AND j.appointment.appointmentTime = :time AND j.status NOT IN ('CANCELLED', 'COMPLETED')")
    long countConflictingJobs(@org.springframework.data.repository.query.Param("staff") User staff, @org.springframework.data.repository.query.Param("date") java.time.LocalDate date, @org.springframework.data.repository.query.Param("time") java.time.LocalTime time);


    @org.springframework.data.jpa.repository.Query(
            "SELECT j FROM Job j WHERE j.assignedStaff = :staff " +
                    "AND j.appointment.appointmentDate = :date " +
                    "AND j.appointment.appointmentTime = :time " +
                    "AND j.status NOT IN ('CANCELLED', 'COMPLETED')")
    List<Job> findConflictingJobs(
            @org.springframework.data.repository.query.Param("staff") User staff,
            @org.springframework.data.repository.query.Param("date") java.time.LocalDate date,
            @org.springframework.data.repository.query.Param("time") java.time.LocalTime time);

}

