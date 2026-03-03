package com.example.support.web;

import com.example.support.SupportSystem;
import com.example.support.Ticket;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class CustomerController {

    private final SupportSystem supportSystem;

    public CustomerController(SupportSystem supportSystem) {
        this.supportSystem = supportSystem;
    }

    @GetMapping("/customer/new")
    public String showCreateTicketForm() {
        return "customer-new-ticket";
    }

    @PostMapping("/customer/new")
    public String createTicket(@RequestParam String name,
                               @RequestParam String subject,
                               @RequestParam String description,
                               Model model) {
        Ticket ticket = supportSystem.createTicket(name, subject, description);
        model.addAttribute("ticket", ticket);
        return "customer-ticket-created";
    }

    @GetMapping("/customer/my-tickets")
    public String viewMyTickets(@RequestParam(required = false) String name, Model model) {
        if (name != null && !name.isBlank()) {
            List<Ticket> tickets = supportSystem.findTicketsByCustomer(name);
            model.addAttribute("tickets", tickets);
            model.addAttribute("name", name);
        }
        return "customer-my-tickets";
    }

    @GetMapping("/customer/feedback/{id}")
    public String showFeedbackForm(@PathVariable("id") int id, Model model) {
        model.addAttribute("ticketId", id);
        return "customer-feedback";
    }

    @PostMapping("/customer/feedback")
    public String submitFeedback(@RequestParam int ticketId,
                                 @RequestParam int rating,
                                 @RequestParam(required = false) String comments,
                                 Model model) {
        boolean ok = supportSystem.addFeedbackToTicket(ticketId, rating, comments == null ? "" : comments);
        model.addAttribute("success", ok);
        model.addAttribute("ticketId", ticketId);
        return "customer-feedback-result";
    }
}

