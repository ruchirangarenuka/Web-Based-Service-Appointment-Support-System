package com.example.sas.service;

import com.example.sas.entity.LeaveRequest;
import com.example.sas.entity.User;
import com.example.sas.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;

    public LeaveRequest applyLeave(User staff, LocalDate startDate, LocalDate endDate, String reason) {
        LeaveRequest leave = new LeaveRequest();
        leave.setStaff(staff);
        leave.setStartDate(startDate);
        leave.setEndDate(endDate);
        leave.setReason(reason);
        leave.setStatus(LeaveRequest.Status.PENDING);
        return leaveRequestRepository.save(leave);
    }

    public List<LeaveRequest> findByStaff(User staff) {
        return leaveRequestRepository.findByStaffOrderByCreatedAtDesc(staff);
    }

    public List<LeaveRequest> findAll() {
        return leaveRequestRepository.findAll();
    }

    public Optional<LeaveRequest> findById(Long id) {
        return leaveRequestRepository.findById(id);
    }

    public LeaveRequest review(Long leaveId, LeaveRequest.Status status, String note, User admin) {
        LeaveRequest leave = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));
        leave.setStatus(status);
        leave.setAdminNote(note);
        leave.setReviewedBy(admin);
        leave.setReviewedAt(LocalDateTime.now());
        return leaveRequestRepository.save(leave);
    }

    public long countByStatus(LeaveRequest.Status status) {
        return leaveRequestRepository.countByStatus(status);
    }
}
