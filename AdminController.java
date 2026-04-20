package com.example.sas.controller;

import com.example.sas.entity.*;
import com.example.sas.repository.SentSmsRepository;
import com.example.sas.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final int MIN_SERVICE_NAME_LENGTH = 3;
    private static final int MAX_SERVICE_NAME_LENGTH = 120;
    private static final int MAX_SERVICE_DESCRIPTION_LENGTH = 1000;
    private static final BigDecimal MAX_SERVICE_PRICE = new BigDecimal("1000000");
    private static final long MAX_SERVICE_IMAGE_BYTES = 5L * 1024L * 1024L;

    private final UserService userService;
    private final ServiceService serviceService;
    private final ServiceCategoryService serviceCategoryService;
    private final AppointmentService appointmentService;
    private final JobService jobService;
    private final PaymentService paymentService;
    private final SupportTicketService supportTicketService;
    private final LeaveRequestService leaveRequestService;
    private final FeedbackService feedbackService;
    private final SmsService smsService;
    private final SentSmsRepository sentSmsRepository;

    private User getCurrentUser(Authentication auth) {
        return userService.findByEmail(auth.getName()).orElseThrow();
    }

    // ─── DASHBOARD ────────────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalCustomers", userService.countByRole(User.Role.CUSTOMER));
        model.addAttribute("totalStaff", userService.countByRole(User.Role.STAFF));
        model.addAttribute("pendingAppointments", appointmentService.countByStatus(Appointment.Status.PENDING));
        model.addAttribute("completedJobs", jobService.countByStatus(Job.Status.COMPLETED));
        model.addAttribute("openTickets", supportTicketService.countByStatus(SupportTicket.Status.OPEN));
        model.addAttribute("pendingLeaves", leaveRequestService.countByStatus(LeaveRequest.Status.PENDING));
        model.addAttribute("totalRevenue", paymentService.getTotalRevenue());
        model.addAttribute("pendingReceipts", paymentService.findPendingReceipts().size());
        model.addAttribute("recentAppointments", appointmentService.findAll().stream().limit(5).toList());

        // ── Payment chart data ────────────────────────────────────────────────
        List<Payment> allPayments = paymentService.findAll();

        // 1. Status counts
        model.addAttribute("paymentStatusPaid",
                allPayments.stream().filter(p -> p.getStatus() == Payment.Status.PAID).count());
        model.addAttribute("paymentStatusPending",
                allPayments.stream().filter(p -> p.getStatus() == Payment.Status.PENDING).count());
        model.addAttribute("paymentStatusReceiptUploaded",
                allPayments.stream().filter(p -> p.getStatus() == Payment.Status.RECEIPT_UPLOADED).count());
        model.addAttribute("paymentStatusFailed",
                allPayments.stream().filter(p -> p.getStatus() == Payment.Status.FAILED).count());

        // 2. Revenue by method (Cash and Bank Transfer only)
        model.addAttribute("cardRevenue", allPayments.stream()
                .filter(p -> p.getStatus() == Payment.Status.PAID
                        && p.getMethod() == Payment.Method.CARD)
                .mapToDouble(p -> p.getAmount().doubleValue()).sum());
        model.addAttribute("bankRevenue", allPayments.stream()
                .filter(p -> p.getStatus() == Payment.Status.PAID
                        && p.getMethod() == Payment.Method.BANK_TRANSFER)
                .mapToDouble(p -> p.getAmount().doubleValue()).sum());

        // 3. Monthly revenue trend — last 6 months
        List<String> labels = new ArrayList<>();
        List<Double> revenue = new ArrayList<>();
        List<Long> counts = new ArrayList<>();

        LocalDate now = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            int y = month.getYear();
            int m = month.getMonthValue();
            labels.add(month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));

            List<Payment> monthPaid = allPayments.stream()
                    .filter(p -> p.getStatus() == Payment.Status.PAID
                            && p.getCreatedAt() != null
                            && p.getCreatedAt().getYear() == y
                            && p.getCreatedAt().getMonthValue() == m)
                    .collect(Collectors.toList());

            revenue.add(monthPaid.stream().mapToDouble(p -> p.getAmount().doubleValue()).sum());
            counts.add((long) monthPaid.size());
        }

        try {
            ObjectMapper om = new ObjectMapper();
            model.addAttribute("monthLabels", om.writeValueAsString(labels));
            model.addAttribute("monthRevenue", om.writeValueAsString(revenue));
            model.addAttribute("monthCount", om.writeValueAsString(counts));
        } catch (Exception e) {
            model.addAttribute("monthLabels", "[\"Jan\",\"Feb\",\"Mar\",\"Apr\",\"May\",\"Jun\"]");
            model.addAttribute("monthRevenue", "[0,0,0,0,0,0]");
            model.addAttribute("monthCount", "[0,0,0,0,0,0]");
        }
        // ── End payment chart data ────────────────────────────────────────────

        return "admin/dashboard";

    }

    // ─── USERS ────────────────────────────────────────────────────────────────
    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.findAllUsers());
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable("id") Long id, RedirectAttributes flash) {
        userService.toggleUserStatus(id);
        flash.addFlashAttribute("success", "User status updated.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable("id") Long id, RedirectAttributes flash) {
        userService.deleteUser(id);
        flash.addFlashAttribute("success", "User deleted.");
        return "redirect:/admin/users";
    }

    // ─── SERVICES ─────────────────────────────────────────────────────────────
    @GetMapping("/services")
    public String services(Model model) {
        serviceCategoryService.syncFromExistingServices();
        model.addAttribute("services", serviceService.findAllServices());
        model.addAttribute("serviceCategories", serviceCategoryService.findAll());
        model.addAttribute("service", new ServiceEntity());
        return "admin/services";
    }

    @PostMapping("/service-categories/add")
    public String addServiceCategory(@RequestParam("name") String name, RedirectAttributes flash) {
        serviceCategoryService.create(name);
        flash.addFlashAttribute("success", "Category saved.");
        return "redirect:/admin/services";
    }

    @PostMapping("/service-categories/{id}/update")
    public String updateServiceCategory(@PathVariable("id") Long id,
            @RequestParam("name") String name,
            RedirectAttributes flash) {
        serviceCategoryService.update(id, name);
        flash.addFlashAttribute("success", "Category updated.");
        return "redirect:/admin/services";
    }

    @PostMapping("/service-categories/{id}/delete")
    public String deleteServiceCategory(@PathVariable("id") Long id, RedirectAttributes flash) {
        serviceCategoryService.delete(id);
        flash.addFlashAttribute("success", "Category deleted.");
        return "redirect:/admin/services";
    }

    @PostMapping("/services/save")
    public String saveService(@ModelAttribute("service") ServiceEntity service,
            @RequestParam(name = "durationHours", required = false) Double durationHours,
            @RequestParam(name = "serviceImage", required = false) MultipartFile serviceImage,
            @RequestParam(name = "clearImage", required = false, defaultValue = "0") String clearImage,
            RedirectAttributes flash) {
        ServiceEntity target;
        if (service.getId() != null) {
            target = serviceService.findById(service.getId()).orElse(null);
            if (target == null) {
                flash.addFlashAttribute("error", "Service not found.");
                return "redirect:/admin/services";
            }
        } else {
            target = new ServiceEntity();
        }

        String category = safeTrim(service.getCategory());
        String name = safeTrim(service.getName());
        String description = safeTrim(service.getDescription());
        BigDecimal price = service.getPrice();
        Integer durationMinutes = convertHoursToMinutes(durationHours);

        String validationError = validateServicePayload(category, name, description, price,
                durationMinutes,
                serviceImage);
        if (validationError != null) {
            flash.addFlashAttribute("error", validationError);
            return "redirect:/admin/services";
        }

        target.setCategory(category);
        target.setName(name);
        target.setDescription(description.isBlank() ? null : description);
        target.setPrice(price);
        target.setDurationMinutes(durationMinutes);

        // Handle image removal
        if ("1".equals(clearImage)) {
            target.setImageUrl(null);
        }
        // Handle image upload (only if not clearing)
        else if (serviceImage != null && !serviceImage.isEmpty()) {
            if (!isAllowedImageType(serviceImage.getContentType())) {
                flash.addFlashAttribute("error", "Please upload a valid image file.");
                return "redirect:/admin/services";
            }
            if (serviceImage.getSize() > MAX_SERVICE_IMAGE_BYTES) {
                flash.addFlashAttribute("error", "Image size must be 5 MB or less.");
                return "redirect:/admin/services";
            }
            try {
                String originalName = serviceImage.getOriginalFilename();
                String ext = ".jpg";
                if (originalName != null && originalName.contains(".")) {
                    ext = originalName.substring(originalName.lastIndexOf('.')).toLowerCase();
                }
                Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "service-images");
                Files.createDirectories(uploadDir);
                String filename = "service_" + UUID.randomUUID() + ext;
                Path filePath = uploadDir.resolve(filename);
                Files.copy(serviceImage.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                target.setImageUrl("/uploads/service-images/" + filename);
            } catch (IOException ex) {
                flash.addFlashAttribute("error", "Failed to upload service image.");
                return "redirect:/admin/services";
            }
        }

        serviceService.save(target);
        flash.addFlashAttribute("success", "Service saved.");
        return "redirect:/admin/services";
    }

    private String validateServicePayload(String category,
            String name,
            String description,
            BigDecimal price,
            Integer durationMinutes,
            MultipartFile serviceImage) {
        if (category.isBlank()) {
            return "Please select a category.";
        }
        boolean categoryExists = serviceCategoryService.findAll().stream()
                .map(ServiceCategory::getName)
                .anyMatch(existing -> existing != null && existing.equalsIgnoreCase(category));
        if (!categoryExists) {
            return "Please select a valid category.";
        }

        if (name.isBlank()) {
            return "Service name is required.";
        }
        if (name.matches("\\d+")) {
            return "Service name cannot contain only numbers.";
        }
        if (name.length() < MIN_SERVICE_NAME_LENGTH) {
            return "Service name must be at least 3 characters.";
        }
        if (name.length() > MAX_SERVICE_NAME_LENGTH) {
            return "Service name cannot exceed 120 characters.";
        }

        if (description.length() > MAX_SERVICE_DESCRIPTION_LENGTH) {
            return "Description cannot exceed 1000 characters.";
        }

        if (price == null) {
            return "Price is required.";
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            return "Price must be greater than 0.";
        }
        if (price.compareTo(MAX_SERVICE_PRICE) > 0) {
            return "Price must be 1,000,000 or less.";
        }

        if (serviceImage != null && !serviceImage.isEmpty()) {
            if (!isAllowedImageType(serviceImage.getContentType())) {
                return "Please upload a valid image file.";
            }
            if (serviceImage.getSize() > MAX_SERVICE_IMAGE_BYTES) {
                return "Image size must be 5 MB or less.";
            }
        }

        return null;
    }

    private boolean isAllowedImageType(String contentType) {
        if (contentType == null) {
            return false;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        return normalized.equals("image/jpeg")
                || normalized.equals("image/jpg")
                || normalized.equals("image/png")
                || normalized.equals("image/webp")
                || normalized.equals("image/gif");
    }

    private Integer convertHoursToMinutes(Double durationHours) {
        if (durationHours == null) {
            return null;
        }
        return (int) Math.round(durationHours * 60);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    @PostMapping("/services/{id}/toggle")
    public String toggleService(@PathVariable("id") Long id, RedirectAttributes flash) {
        serviceService.toggleStatus(id);
        flash.addFlashAttribute("success", "Service status updated.");
        return "redirect:/admin/services";
    }

    @PostMapping("/services/{id}/delete")
    public String deleteService(@PathVariable("id") Long id, RedirectAttributes flash) {
        serviceService.delete(id);
        flash.addFlashAttribute("success", "Service deleted.");
        return "redirect:/admin/services";
    }

    // ─── APPOINTMENTS ─────────────────────────────────────────────────────────
    @GetMapping("/appointments")
    public String appointments(Model model) {
        model.addAttribute("appointments", appointmentService.findAll());
        return "admin/appointments";
    }

    @PostMapping("/appointments/{id}/status")
    public String updateAppointmentStatus(@PathVariable("id") Long id,
            @RequestParam("status") String status,
            RedirectAttributes flash) {
        Appointment.Status newStatus = Appointment.Status.valueOf(status);
        Appointment appointment = appointmentService.updateStatus(id, newStatus);
        
        if (newStatus == Appointment.Status.CONFIRMED) {
            smsService.sendBookingConfirmationSms(appointment);
        }
        
        flash.addFlashAttribute("success", "Appointment status updated.");
        return "redirect:/admin/appointments";
    }

    // ─── JOBS ─────────────────────────────────────────────────────────────────
    @GetMapping("/jobs")
    public String jobs(Model model) {
        model.addAttribute("jobs", jobService.findAll());
        model.addAttribute("staffList", userService.findByRole(User.Role.STAFF));
        return "admin/jobs";
    }

    @GetMapping("/jobs/history")
    public String jobHistory(Model model) {
        model.addAttribute("jobs", jobService.findAll());
        return "admin/job-history";
    }

    @GetMapping("/jobs/{id}/details")
    public String jobDetails(@PathVariable("id") Long id, Model model) {
        Job job = jobService.findById(id).orElseThrow(() -> new RuntimeException("Job not found"));
        model.addAttribute("job", job);
        
        java.util.Optional<Payment> payment = paymentService.findByAppointmentId(job.getAppointment().getId());
        model.addAttribute("payment", payment.orElse(null));
        
        java.util.Optional<Feedback> feedback = feedbackService.findByAppointmentId(job.getAppointment().getId());
        model.addAttribute("feedback", feedback.orElse(null));
        
        return "admin/job-details";
    }

    @PostMapping("/jobs/{id}/assign")
    public String assignJob(@PathVariable("id") Long id,
                            @RequestParam(name = "staffId", required = false) Long staffId,
                            RedirectAttributes flash) {
        if (staffId == null) {
            flash.addFlashAttribute("error", "Please select a staff member to assign.");
            return "redirect:/admin/jobs";
        }
        try {
            Job assignedJob = jobService.assignStaff(id, staffId);

            String assignedName = assignedJob.getAssignedStaff().getFullName();
            String requestedName = userService.findById(staffId)
                    .map(User::getFullName).orElse("selected staff");

            if (!assignedJob.getAssignedStaff().getId().equals(staffId)) {
                flash.addFlashAttribute("error",
                        "⚠ Double booking detected! " + requestedName +
                                " is already booked at this time. Auto-assigned to: " + assignedName + " instead.");
            } else {
                flash.addFlashAttribute("success", "Staff assigned: " + assignedName);
            }

            // Send SMS notification to customer
            if (assignedJob.getAppointment() != null &&
                    assignedJob.getAppointment().getStatus() == Appointment.Status.CONFIRMED) {
                smsService.sendBookingConfirmationSms(assignedJob.getAppointment());
            }

        } catch (RuntimeException ex) {
            flash.addFlashAttribute("error", "❌ " + ex.getMessage());
        }
        return "redirect:/admin/jobs";
    }

    // ─── LEAVE REQUESTS ───────────────────────────────────────────────────────
    @GetMapping("/leave-requests")
    public String leaveRequests(Model model) {
        model.addAttribute("leaves", leaveRequestService.findAll());
        return "admin/leave-requests";
    }

    @PostMapping("/leave-requests/{id}/review")
    public String reviewLeave(@PathVariable("id") Long id,
            @RequestParam("status") String status,
            @RequestParam(name = "note", required = false) String note,
            RedirectAttributes flash,
            Authentication auth) {
        leaveRequestService.review(id, LeaveRequest.Status.valueOf(status), note, getCurrentUser(auth));
        flash.addFlashAttribute("success", "Leave request updated.");
        return "redirect:/admin/leave-requests";
    }

    // ─── SUPPORT TICKETS ──────────────────────────────────────────────────────
    @GetMapping("/support-tickets")
    public String supportTickets(Model model) {
        model.addAttribute("tickets", supportTicketService.findAll());
        return "admin/support-tickets";
    }

    @PostMapping("/support-tickets/{id}/respond")
    public String respondTicket(@PathVariable("id") Long id,
            @RequestParam("response") String response,
            @RequestParam("status") String status,
            RedirectAttributes flash,
            Authentication auth) {
        supportTicketService.respond(id, response, SupportTicket.Status.valueOf(status), getCurrentUser(auth));
        flash.addFlashAttribute("success", "Response sent and status updated.");
        return "redirect:/admin/support-tickets";
    }

    @PostMapping("/support-tickets/{id}/status")
    public String updateTicketStatus(@PathVariable("id") Long id,
            @RequestParam("status") String status,
            RedirectAttributes flash) {
        supportTicketService.updateStatus(id, SupportTicket.Status.valueOf(status));
        flash.addFlashAttribute("success", "Ticket status updated.");
        return "redirect:/admin/support-tickets";
    }

    // ─── SMS LOGS ─────────────────────────────────────────────────────────────
    @GetMapping("/sms-logs")
    public String smsLogs(Model model) {
        model.addAttribute("smsLogs", sentSmsRepository.findAllByOrderBySentAtDesc());
        return "admin/sms-logs";
    }
}
