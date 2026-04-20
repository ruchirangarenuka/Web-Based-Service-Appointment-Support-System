package com.example.sas.repository;

import com.example.sas.entity.SupportTicket;
import com.example.sas.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByCustomer(User customer);
    List<SupportTicket> findByCustomerOrderByCreatedAtDesc(User customer);
    List<SupportTicket> findByStatus(SupportTicket.Status status);
    long countByStatus(SupportTicket.Status status);
}
