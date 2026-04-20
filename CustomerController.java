package com.example.sas.controller;

import com.example.sas.dto.AppointmentDto;
import com.example.sas.dto.UserProfileUpdateDto;
import com.example.sas.entity.*;
import com.example.sas.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final UserService userService;
    private final ServiceService serviceService;
    private final AppointmentService appointmentService;
    private final JobService jobService;
    private final PaymentService paymentService;
    private final SupportTicketService supportTicketService;
    private final FeedbackService feedbackService;

    private User getCurrentUser(Authentication auth) {
        return userService.findByEmail(auth.getName()).orElseThrow();
    }

    // ─── DASHBOARD ────────────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        User user = getCurrentUser(auth);
        var appointments = appointmentService.findByCustomer(user);
        model.addAttribute("appointments", appointments);
        model.addAttribute("totalAppointments", appointments.size());
        model.addAttribute("pendingCount", appointments.stream()
                .filter(a -> a.getStatus() == Appointment.Status.PENDING).count());
        model.addAttribute("completedCount", appointments.stream()
                .filter(a -> a.getStatus() == Appointment.Status.COMPLETED).count());
        model.addAttribute("recentAppointments", appointments.stream().limit(3).toList());
        return "customer/dashboard";
    }

    // ─── SERVICES ─────────────────────────────────────────────────────────────
    @GetMapping("/services")
    public String services(Model model) {
        model.addAttribute("services", serviceService.findActiveServices());
        return "customer/services";
    }

    // ─── BOOK APPOINTMENT ─────────────────────────────────────────────────────
    @GetMapping("/book-appointment")
    public String bookPage(@RequestParam(name = "serviceId", required = false) Long serviceId, Model model) {
        model.addAttribute("services", serviceService.findActiveServices());
        model.addAttribute("dto", new AppointmentDto());
        if (serviceId != null) model.addAttribute("selectedServiceId", serviceId);
        return "customer/book-appointment";
    }

    @PostMapping("/book-appointment")
    public String bookAppointment(@Valid @ModelAttribute("dto") AppointmentDto dto,
                                   BindingResult result,
                                   Model model,
                                   Authentication auth,
                                   RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("services", serviceService.findActiveServices());
            return "customer/book-appointment";
        }
        try {
            User user = getCurrentUser(auth);
            Appointment apt = appointmentService.bookAppointment(user, dto);
            // Redirect directly to payment page after booking
            flash.addFlashAttribute("newBooking", true);
            flash.addFlashAttribute("success", "Appointment booked! Please complete your payment.");
            return "redirect:/customer/payment/" + apt.getId();
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("services", serviceService.findActiveServices());
            return "customer/book-appointment";
        }
    }

    // ─── PAYMENT FOR APPOINTMENT ───────────────────────────────────────────────
    @GetMapping("/payment/{appointmentId}")
    public String paymentPage(@PathVariable("appointmentId") Long appointmentId, Model model, Authentication auth) {
        Appointment apt = appointmentService.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        // Security: only owner
        if (!apt.getCustomer().getEmail().equals(auth.getName())) {
            return "redirect:/customer/my-appointments";
        }
        // Check if payment already exists
        var existingPayment = paymentService.findByAppointmentId(appointmentId);
        model.addAttribute("appointment", apt);
        model.addAttribute("existingPayment", existingPayment.orElse(null));
        return "customer/payment-checkout";
    }

    @PostMapping("/payment/card")
    public String payByCard(@RequestParam("appointmentId") Long appointmentId,
                             @RequestParam(value = "cvv", required = false) String cvv,
                             @RequestParam(value = "expiry", required = false) String expiry,
                             Authentication auth,
                             RedirectAttributes flash) {
        try {
            // Basic validation for CVV and Expiry
            if (cvv != null && cvv.length() != 3) {
                throw new RuntimeException("CVV must be 3 digits");
            }
            if (expiry != null && !expiry.matches("(0[1-9]|1[0-2])/[0-9]{2}")) {
                throw new RuntimeException("Expiry date must be in MM/YY format");
            }
            
            Payment p = paymentService.createPayment(appointmentId, Payment.Method.CARD);
            paymentService.processCardPayment(p.getId());
            flash.addFlashAttribute("success", "Card payment successful! Your appointment is confirmed.");
            return "redirect:/customer/payment/invoice/" + p.getId();
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/customer/payment/" + appointmentId;
        }
    }

    @PostMapping("/payment/bank-transfer")
    public String payByBankTransfer(@RequestParam("appointmentId") Long appointmentId,
                                     @RequestParam("receipt") MultipartFile receipt,
                                     Authentication auth,
                                     RedirectAttributes flash) {
        try {
            Payment p = paymentService.createPayment(appointmentId, Payment.Method.BANK_TRANSFER);
            paymentService.uploadReceipt(p.getId(), receipt);
            flash.addFlashAttribute("success", "Receipt uploaded! Awaiting admin confirmation.");
            return "redirect:/customer/payment/invoice/" + p.getId();
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/customer/payment/" + appointmentId;
        }
    }

    // ─── INVOICE PAGE ──────────────────────────────────────────────────────────
    @GetMapping("/payment/invoice/{paymentId}")
    public String invoicePage(@PathVariable("paymentId") Long paymentId, Model model, Authentication auth) {
        Payment payment = paymentService.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        model.addAttribute("payment", payment);
        model.addAttribute("job", jobService.findByAppointmentId(payment.getAppointment().getId()).orElse(null));
        return "customer/invoice";
    }

    // ─── MY APPOINTMENTS ──────────────────────────────────────────────────────
    @GetMapping("/my-appointments")
    public String myAppointments(Model model, Authentication auth) {
        User user = getCurrentUser(auth);
        var appointments = appointmentService.findByCustomer(user);
        model.addAttribute("appointments", appointments);
        // map appointmentId → payment
        var paymentMap = new java.util.HashMap<Long, Payment>();
        for (var apt : appointments) {
            paymentService.findByAppointmentId(apt.getId())
                    .ifPresent(p -> paymentMap.put(apt.getId(), p));
        }
        var jobMap = new java.util.HashMap<Long, Job>();
        for (var apt : appointments) {
            jobService.findByAppointmentId(apt.getId())
                    .ifPresent(j -> jobMap.put(apt.getId(), j));
        }
        model.addAttribute("paymentMap", paymentMap);
        model.addAttribute("jobMap", jobMap);
        return "customer/my-appointments";
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancelAppointment(@PathVariable("id") Long id,
                                   @RequestParam(value="bankName", required=false) String bankName,
                                   @RequestParam(value="accNo", required=false) String accNo,
                                   @RequestParam(value="holderName", required=false) String holderName,
                                   @RequestParam(value="branch", required=false) String branch,
                                   Authentication auth, RedirectAttributes flash) {
        try {
            appointmentService.cancel(id, getCurrentUser(auth), bankName, accNo, holderName, branch);
            flash.addFlashAttribute("success", "Appointment cancelled successfully. Refund will be processed soon.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customer/my-appointments";
    }

    // ─── PAYMENTS HISTORY ─────────────────────────────────────────────────────
    @GetMapping("/payments")
    public String payments(Model model, Authentication auth) {
        User user = getCurrentUser(auth);
        var payments = paymentService.findByCustomer(user);
        var paymentJobMap = new java.util.HashMap<Long, Job>();
        for (var pay : payments) {
            if (pay.getAppointment() == null) {
                continue;
            }
            jobService.findByAppointmentId(pay.getAppointment().getId())
                    .ifPresent(j -> paymentJobMap.put(pay.getId(), j));
        }
        model.addAttribute("payments", payments);
        model.addAttribute("paymentJobMap", paymentJobMap);
        return "customer/payments";
    }

    // ─── FEEDBACK ─────────────────────────────────────────────────────────────
    // Feedback only allowed after payment is PAID
    @GetMapping("/feedback")
    public String feedback(Model model, Authentication auth) {
        User user = getCurrentUser(auth);
        model.addAttribute("feedbackList", feedbackService.findByCustomer(user));
        // Only PAID appointments that don't yet have feedback
        var paidAppointments = appointmentService.findByCustomer(user).stream()
                .filter(a -> a.getStatus() == Appointment.Status.COMPLETED ||
                             a.getStatus() == Appointment.Status.CONFIRMED)
                .filter(a -> {
                    var payment = paymentService.findByAppointmentId(a.getId());
                    return payment.isPresent() && payment.get().getStatus() == Payment.Status.PAID;
                })
                .filter(a -> !feedbackService.existsByAppointmentId(a.getId()))
                .toList();
        model.addAttribute("paidAppointments", paidAppointments);
        model.addAttribute("publicFeedbacks", feedbackService.getPublicFeedbacks());
        return "customer/feedback";
    }

    @PostMapping("/feedback/submit")
    public String submitFeedback(@RequestParam(value = "appointmentId", required = false) Long appointmentId,
                                  @RequestParam(value = "rating", required = false, defaultValue = "0") Integer rating,
                                  @RequestParam(value = "comment", required = false) String comment,
                                  Authentication auth,
                                  RedirectAttributes flash) {
        if (appointmentId == null) {
            flash.addFlashAttribute("error", "Please select an appointment before submitting your review.");
            return "redirect:/customer/feedback";
        }
        if (rating == null || rating == 0) {
            flash.addFlashAttribute("error", "Please select a star rating.");
            return "redirect:/customer/feedback";
        }
        try {
            feedbackService.submitFeedback(getCurrentUser(auth), appointmentId, rating, comment);
            return "redirect:/customer/feedback/success";
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/customer/feedback";
        }
    }

    @PostMapping("/feedback/edit")
    public String editFeedback(@RequestParam("id") Long id,
                               @RequestParam(value = "rating", required = false, defaultValue = "0") Integer rating,
                               @RequestParam(value = "comment", required = false) String comment,
                               Authentication auth,
                               RedirectAttributes flash) {
        if (rating == null || rating == 0) {
            flash.addFlashAttribute("error", "Please provide a valid rating.");
            return "redirect:/customer/feedback";
        }
        try {
            feedbackService.updateFeedback(id, getCurrentUser(auth), rating, comment);
            flash.addFlashAttribute("success", "Review updated successfully.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customer/feedback";
    }

    @PostMapping("/feedback/delete")
    public String deleteFeedback(@RequestParam("id") Long id,
                                 Authentication auth,
                                 RedirectAttributes flash) {
        try {
            feedbackService.deleteFeedback(id, getCurrentUser(auth));
            flash.addFlashAttribute("success", "Review deleted successfully.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customer/feedback";
    }

    @GetMapping("/feedback/success")
    public String feedbackSuccess() {
        return "customer/feedback-success";
    }

    // ─── SUPPORT ──────────────────────────────────────────────────────────────
    @GetMapping("/support")
    public String supportHome(Model model, Authentication auth) {
        return "customer/support-home";
    }

    @GetMapping("/support/tickets")
    public String supportTickets(@RequestParam(value = "searchName", required = false) String searchName, Model model, Authentication auth) {
        User user = getCurrentUser(auth);
        java.util.List<SupportTicket> tickets = supportTicketService.findByCustomer(user);
        
        if (searchName != null && !searchName.trim().isEmpty()) {
            final String q = searchName.trim().toLowerCase();
            tickets = tickets.stream()
                .filter(t -> (t.getCustomerName() != null && t.getCustomerName().toLowerCase().contains(q)) ||
                             (t.getSubject() != null && t.getSubject().toLowerCase().contains(q)))
                .toList();
        }
        
        model.addAttribute("tickets", tickets);
        model.addAttribute("searchName", searchName);
        return "customer/support";
    }

    @PostMapping("/support/create")
    public String createTicket(@RequestParam("subject") String subject,
                                @RequestParam("description") String description,
                                @RequestParam("priority") String priority,
                                @RequestParam("customerName") String customerName,
                                @RequestParam("customerIdNumber") String customerIdNumber,
                                @RequestParam(value = "evidence", required = false) MultipartFile evidence,
                                Authentication auth,
                                RedirectAttributes flash) {
        if (subject.matches(".*\\d.*")) {
            flash.addFlashAttribute("error", "Cannot use numbers in subject");
            return "redirect:/customer/support";
        }
        
        try {
            supportTicketService.createTicket(getCurrentUser(auth), subject, description,
                    SupportTicket.Priority.valueOf(priority), customerName, customerIdNumber, evidence);
            flash.addFlashAttribute("success", "Support ticket created.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customer/support/tickets";
    }

    // ─── PROFILE ──────────────────────────────────────────────────────────────
    @GetMapping("/profile")
    public String profile(Model model, Authentication auth) {
        User user = getCurrentUser(auth);
        UserProfileUpdateDto dto = new UserProfileUpdateDto();
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        model.addAttribute("dto", dto);
        return "customer/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@jakarta.validation.Valid @ModelAttribute("dto") UserProfileUpdateDto dto,
                                 BindingResult result,
                                 Authentication auth,
                                 RedirectAttributes flash,
                                 Model model) {
        if (result.hasErrors()) {
            model.addAttribute("currentUser", getCurrentUser(auth));
            return "customer/profile";
        }
        try {
            userService.updateProfile(getCurrentUser(auth).getId(), dto);
            flash.addFlashAttribute("success", "Profile updated successfully.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customer/profile";
    }

    @GetMapping("/change-password")
    public String changePasswordPage(Model model) {
        model.addAttribute("dto", new com.example.sas.dto.PasswordChangeDto());
        return "customer/change-password";
    }

    @PostMapping("/change-password")
    public String handleChangePassword(@Valid @ModelAttribute("dto") com.example.sas.dto.PasswordChangeDto dto,
                                       BindingResult result,
                                       Authentication auth,
                                       RedirectAttributes flash) {
        if (result.hasErrors()) {
            return "customer/change-password";
        }
        try {
            User user = getCurrentUser(auth);
            // We can reuse the userService.updateProfile logic by creating a partial DTO 
            // or better yet, add a dedicated method. I'll use a partial DTO for now.
            UserProfileUpdateDto profileDto = new UserProfileUpdateDto();
            profileDto.setFullName(user.getFullName());
            profileDto.setCurrentPassword(dto.getCurrentPassword());
            profileDto.setNewPassword(dto.getNewPassword());
            profileDto.setConfirmNewPassword(dto.getConfirmNewPassword());
            
            userService.updateProfile(user.getId(), profileDto);
            flash.addFlashAttribute("success", "Password changed successfully!");
            return "redirect:/customer/profile";
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/customer/change-password";
        }
    }

    // REST endpoint to get available workers for a service and time slot
    @GetMapping("/api/available-workers")
    @ResponseBody
    public List<User> getAvailableWorkers(@RequestParam("serviceType") String serviceType,
                                          @RequestParam("date") String dateStr,
                                          @RequestParam("time") String timeStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            LocalTime time = LocalTime.parse(timeStr);
            return userService.findAvailableWorkers(serviceType, date, time);
        } catch (Exception e) {
            // Return empty list if parsing fails
            return List.of();
        }
    }
}
