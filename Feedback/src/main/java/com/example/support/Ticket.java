package com.example.support;

import java.time.LocalDateTime;

public class Ticket {
    private static int counter = 1;

    private final int id;
    private String customerName;
    private String subject;
    private String description;
    private TicketStatus status;
    private String adminReply;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Feedback feedback;

    public Ticket(String customerName, String subject, String description) {
        this.id = counter++;
        this.customerName = customerName;
        this.subject = subject;
        this.description = description;
        this.status = TicketStatus.OPEN;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public int getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getSubject() {
        return subject;
    }

    public String getDescription() {
        return description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public String getAdminReply() {
        return adminReply;
    }

    public void setAdminReply(String adminReply) {
        this.adminReply = adminReply;
        this.updatedAt = LocalDateTime.now();
    }

    public Feedback getFeedback() {
        return feedback;
    }

    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "Ticket #" + id +
                " | Customer: " + customerName +
                " | Subject: " + subject +
                " | Status: " + status +
                (adminReply != null ? " | Admin reply: " + adminReply : "") +
                (feedback != null ? " | Rating: " + feedback.getRating() : "");
    }
}

