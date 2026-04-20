package com.example.sas.repository;

import com.example.sas.entity.LeaveRequest;
import com.example.sas.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByStaff(User staff);
    List<LeaveRequest> findByStaffOrderByCreatedAtDesc(User staff);
    List<LeaveRequest> findByStatus(LeaveRequest.Status status);
    List<LeaveRequest> findByStaffAndStartDateBetween(User staff, LocalDate start, LocalDate end);
    long countByStatus(LeaveRequest.Status status);
}
