package com.example.sas.service;

import com.example.sas.entity.Job;
import com.example.sas.entity.User;
import com.example.sas.repository.JobRepository;
import com.example.sas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public List<Job> findAll() {
        return jobRepository.findAll();
    }

    public List<Job> findByStaff(User staff) {
        return jobRepository.findByAssignedStaffOrderByCreatedAtDesc(staff);
    }

    public Optional<Job> findById(Long id) {
        return jobRepository.findById(id);
    }

    public Optional<Job> findByAppointmentId(Long appointmentId) {
        return jobRepository.findByAppointmentId(appointmentId);
    }

    public Job assignStaff(Long jobId, Long staffId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        User requestedStaff = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        LocalDate date = job.getAppointment().getAppointmentDate();
        LocalTime time = job.getAppointment().getAppointmentTime();

        // Check if requested staff is already booked at this date/time
        boolean isConflict = jobRepository.countConflictingJobs(requestedStaff, date, time) > 0;

        User staffToAssign = requestedStaff;

        if (isConflict) {
            // Find all STAFF role users
            List<User> allStaff = userRepository.findByRole(User.Role.STAFF);

            // Auto-assign first available staff with no conflict at this date/time
            staffToAssign = allStaff.stream()
                    .filter(s -> !s.getId().equals(requestedStaff.getId()))
                    .filter(s -> jobRepository.countConflictingJobs(s, date, time) == 0)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(
                            "No available staff at " + date + " " + time +
                                    ". All staff are booked at this time."));
        }

        job.setAssignedStaff(staffToAssign);
        job.setStatus(Job.Status.ASSIGNED);
        return jobRepository.save(job);
    }

    public Job updateStatus(Long jobId, Job.Status status, String notes) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        job.setStatus(status);
        if (notes != null && !notes.isBlank()) {
            job.setStaffNotes(notes);
        }
        if (status == Job.Status.IN_PROGRESS && job.getStartedAt() == null) {
            job.setStartedAt(LocalDateTime.now());
        }
        if (status == Job.Status.COMPLETED) {
            job.setCompletedAt(LocalDateTime.now());
        }

        // ── SYNC STATUS BACK TO APPOINTMENT ──
        if (job.getAppointment() != null) {
            com.example.sas.entity.Appointment apt = job.getAppointment();
            if (status == Job.Status.IN_PROGRESS) {
                apt.setStatus(com.example.sas.entity.Appointment.Status.IN_PROGRESS);
            } else if (status == Job.Status.COMPLETED) {
                apt.setStatus(com.example.sas.entity.Appointment.Status.COMPLETED);
            } else if (status == Job.Status.CANCELLED) {
                apt.setStatus(com.example.sas.entity.Appointment.Status.CANCELLED);
            }
        }

        return jobRepository.save(job);
    }

    public long countByStatus(Job.Status status) {
        return jobRepository.countByStatus(status);
    }

    public long countByStaff(User staff) {
        return jobRepository.countByAssignedStaff(staff);
    }
}