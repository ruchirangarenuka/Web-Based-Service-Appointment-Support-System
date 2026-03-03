package com.example.support.web;

import com.example.support.SupportSystem;
import com.example.support.Ticket;
import com.example.support.TicketStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class AdminController {

    private static final String ADMIN_PASSWORD = "admin123";

    private final SupportSystem supportSystem;

    public AdminController(SupportSystem supportSystem) {
        this.supportSystem = supportSystem;
    }

    @GetMapping("/admin")
    public String adminLogin() {
        return "admin-login";
    }

    @PostMapping("/admin/login")
    public String handleAdminLogin(@RequestParam String password, Model model) {
        if (!ADMIN_PASSWORD.equals(password)) {
            model.addAttribute("error", "Invalid password");
            return "admin-login";
        }
        List<Ticket> tickets = supportSystem.getAllTickets();
        model.addAttribute("tickets", tickets);
        return "admin-dashboard";
    }

    @PostMapping("/admin/reply")
    public String replyToTicket(@RequestParam int ticketId,
                                @RequestParam String reply,
                                @RequestParam String password,
                                Model model) {
        if (!ADMIN_PASSWORD.equals(password)) {
            model.addAttribute("error", "Invalid password");
        } else {
            boolean ok = supportSystem.replyToTicket(ticketId, reply);
            model.addAttribute("message", ok ? "Reply saved." : "Ticket not found.");
        }
        List<Ticket> tickets = supportSystem.getAllTickets();
        model.addAttribute("tickets", tickets);
        return "admin-dashboard";
    }

    @PostMapping("/admin/status")
    public String updateStatus(@RequestParam int ticketId,
                               @RequestParam TicketStatus status,
                               @RequestParam String password,
                               Model model) {
        if (!ADMIN_PASSWORD.equals(password)) {
            model.addAttribute("error", "Invalid password");
        } else {
            boolean ok = supportSystem.updateTicketStatus(ticketId, status);
            model.addAttribute("message", ok ? "Status updated." : "Ticket not found.");
        }
        List<Ticket> tickets = supportSystem.getAllTickets();
        model.addAttribute("tickets", tickets);
        return "admin-dashboard";
    }
}

