package com.example.sas.service;

import com.example.sas.entity.SupportTicket;
import com.example.sas.entity.User;
import com.example.sas.repository.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;
    private static final String UPLOAD_DIR = "uploads/tickets/";

    public SupportTicket createTicket(User customer, String subject, String description, SupportTicket.Priority priority, String customerName, String customerIdNumber, MultipartFile evidence) throws IOException {
        SupportTicket ticket = new SupportTicket();
        ticket.setCustomer(customer);
        ticket.setCustomerName(customerName);
        ticket.setCustomerIdNumber(customerIdNumber);
        ticket.setSubject(subject);
        ticket.setDescription(description);
        ticket.setPriority(priority);
        ticket.setStatus(SupportTicket.Status.OPEN);
        ticket = supportTicketRepository.save(ticket);
        
        if (evidence != null && !evidence.isEmpty()) {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Files.createDirectories(uploadPath);
            String filename = "ticket_" + ticket.getId() + "_" + System.currentTimeMillis()
                    + getExtension(evidence.getOriginalFilename());
            Path filePath = uploadPath.resolve(filename);
            Files.copy(evidence.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            ticket.setAttachmentPath(filename);
            ticket = supportTicketRepository.save(ticket);
        }
        
        return ticket;
    }
    
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf("."));
    }

    public List<SupportTicket> findByCustomer(User customer) {
        return supportTicketRepository.findByCustomerOrderByCreatedAtDesc(customer);
    }

    public List<SupportTicket> findAll() {
        return supportTicketRepository.findAll();
    }

    public Optional<SupportTicket> findById(Long id) {
        return supportTicketRepository.findById(id);
    }

    public SupportTicket respond(Long ticketId, String response, SupportTicket.Status status, User admin) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setAdminResponse(response);
        ticket.setHandledBy(admin);
        ticket.setStatus(status);
        return supportTicketRepository.save(ticket);
    }

    public SupportTicket updateStatus(Long ticketId, SupportTicket.Status status) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setStatus(status);
        return supportTicketRepository.save(ticket);
    }

    public long countByStatus(SupportTicket.Status status) {
        return supportTicketRepository.countByStatus(status);
    }
}
