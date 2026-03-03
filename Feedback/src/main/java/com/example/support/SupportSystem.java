package com.example.support;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SupportSystem {
    private final List<Ticket> tickets = new ArrayList<>();

    public Ticket createTicket(String customerName, String subject, String description) {
        Ticket ticket = new Ticket(customerName, subject, description);
        tickets.add(ticket);
        return ticket;
    }

    public List<Ticket> getAllTickets() {
        return new ArrayList<>(tickets);
    }

    public Optional<Ticket> findTicketById(int id) {
        return tickets.stream().filter(t -> t.getId() == id).findFirst();
    }

    public List<Ticket> findTicketsByCustomer(String customerName) {
        List<Ticket> result = new ArrayList<>();
        for (Ticket ticket : tickets) {
            if (ticket.getCustomerName().equalsIgnoreCase(customerName)) {
                result.add(ticket);
            }
        }
        return result;
    }

    public boolean addFeedbackToTicket(int ticketId, int rating, String comments) {
        Optional<Ticket> optionalTicket = findTicketById(ticketId);
        if (optionalTicket.isEmpty()) {
            return false;
        }
        Ticket ticket = optionalTicket.get();
        Feedback feedback = new Feedback(rating, comments);
        ticket.setFeedback(feedback);
        return true;
    }

    public boolean updateTicketStatus(int ticketId, TicketStatus status) {
        Optional<Ticket> optionalTicket = findTicketById(ticketId);
        if (optionalTicket.isEmpty()) {
            return false;
        }
        optionalTicket.get().setStatus(status);
        return true;
    }

    public boolean replyToTicket(int ticketId, String reply) {
        Optional<Ticket> optionalTicket = findTicketById(ticketId);
        if (optionalTicket.isEmpty()) {
            return false;
        }
        optionalTicket.get().setAdminReply(reply);
        return true;
    }
}

