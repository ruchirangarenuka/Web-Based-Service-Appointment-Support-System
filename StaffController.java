package com.example.sas.controller;

import com.example.sas.dto.UserProfileUpdateDto;
import com.example.sas.entity.*;
import com.example.sas.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

import java.time.LocalDate;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final UserService userService;
    private final JobService jobService;
    private final LeaveRequestService leaveRequestService;
    private final PaymentService paymentService;

    private User getCurrentUser(Authentication auth) {
        return userService.findByEmail(auth.getName()).orElseThrow();
    }

    // ─── DASHBOARD ────────────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        User user = getCurrentUser(auth);
        var jobs = jobService.findByStaff(user);
        model.addAttribute("totalJobs",     jobs.size());
        model.addAttribute("pendingJobs",   jobs.stream()
                .filter(j -> j.getStatus() == Job.Status.PENDING
                          || j.getStatus() == Job.Status.ASSIGNED).count());
        model.addAttribute("completedJobs", jobs.stream()
                .filter(j -> j.getStatus() == Job.Status.COMPLETED).count());
        model.addAttribute("recentJobs",    jobs.stream().limit(5).toList());
        return "staff/dashboard";
    }

    // ─── MY JOBS ──────────────────────────────────────────────────────────────
    @GetMapping("/my-jobs")
    public String myJobs(Model model, Authentication auth) {
        model.addAttribute("jobs", jobService.findByStaff(getCurrentUser(auth)));
        return "staff/my-jobs";
    }

    @PostMapping("/jobs/{id}/status")
    public String updateJobStatus(@PathVariable("id") Long id,
                                   @RequestParam("status") String status,
                                   @RequestParam(name = "notes", required = false) String notes,
                                   RedirectAttributes flash) {
        jobService.updateStatus(id, Job.Status.valueOf(status), notes);
        flash.addFlashAttribute("success", "Job status updated.");
        return "redirect:/staff/my-jobs";
    }

    // ─── SCHEDULE ─────────────────────────────────────────────────────────────
    @GetMapping("/schedule")
    public String schedule(Model model, Authentication auth) {
        User user = getCurrentUser(auth);
        var jobs = jobService.findByStaff(user);
        model.addAttribute("jobs", jobs);
        model.addAttribute("upcomingJobs", jobs.stream()
                .filter(j -> j.getStatus() != Job.Status.COMPLETED
                          && j.getStatus() != Job.Status.CANCELLED)
                .sorted((a, b) -> {
                    LocalDate dateA = a.getAppointment() != null ? a.getAppointment().getAppointmentDate() : LocalDate.MAX;
                    LocalDate dateB = b.getAppointment() != null ? b.getAppointment().getAppointmentDate() : LocalDate.MAX;
                    return dateA.compareTo(dateB);
                })
                .toList());
        model.addAttribute("completedJobs", jobs.stream()
                .filter(j -> j.getStatus() == Job.Status.COMPLETED)
                .toList());
        model.addAttribute("today", LocalDate.now());
        return "staff/schedule";
    }

    // ─── LEAVE ────────────────────────────────────────────────────────────────
    @GetMapping("/leave-apply")
    public String leaveApplyPage(Model model, Authentication auth) {
        User user = getCurrentUser(auth);
        model.addAttribute("leaves", leaveRequestService.findByStaff(user));
        return "staff/leave-apply";
    }

    @PostMapping("/leave-apply")
    public String applyLeave(@RequestParam("startDate") String startDate,
                              @RequestParam("endDate") String endDate,
                              @RequestParam("reason") String reason,
                              Authentication auth,
                              RedirectAttributes flash) {
        try {
            leaveRequestService.applyLeave(
                    getCurrentUser(auth),
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate),
                    reason);
            flash.addFlashAttribute("success", "Leave request submitted.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/leave-apply";
    }

    // ─── PROFILE ──────────────────────────────────────────────────────────────
    @GetMapping("/profile")
    public String profile(Model model, Authentication auth) {
        User user = getCurrentUser(auth);
        UserProfileUpdateDto dto = new UserProfileUpdateDto();
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        
        // Populate availability
        if (user.getAvailability() != null) {
            dto.setAvailability(user.getAvailability().split(", "));
        }
        dto.setAvailableFromTime(user.getAvailableFromTime());
        dto.setAvailableToTime(user.getAvailableToTime());
        
        model.addAttribute("dto", dto);
        model.addAttribute("currentUser", user); // For template visibility
        return "staff/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@jakarta.validation.Valid @ModelAttribute("dto") UserProfileUpdateDto dto,
                                 BindingResult result,
                                 Authentication auth,
                                 RedirectAttributes flash,
                                 Model model) {
        if (result.hasErrors()) {
            model.addAttribute("currentUser", getCurrentUser(auth));
            return "staff/profile";
        }
        try {
            userService.updateProfile(getCurrentUser(auth).getId(), dto);
            flash.addFlashAttribute("success", "Profile updated.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/profile";
    }

    @GetMapping("/change-password")
    public String changePasswordPage(Model model) {
        model.addAttribute("dto", new com.example.sas.dto.PasswordChangeDto());
        return "staff/change-password";
    }

    @PostMapping("/change-password")
    public String handleChangePassword(@ModelAttribute("dto") com.example.sas.dto.PasswordChangeDto dto,
                                       Authentication auth,
                                       RedirectAttributes flash) {
        try {
            User user = getCurrentUser(auth);
            UserProfileUpdateDto profileDto = new UserProfileUpdateDto();
            profileDto.setFullName(user.getFullName());
            profileDto.setCurrentPassword(dto.getCurrentPassword());
            profileDto.setNewPassword(dto.getNewPassword());
            profileDto.setConfirmNewPassword(dto.getConfirmNewPassword());
            
            userService.updateProfile(user.getId(), profileDto);
            flash.addFlashAttribute("success", "Password changed successfully!");
            return "redirect:/staff/profile";
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/staff/change-password";
        }
    }
    // ─── MY PAYMENTS ──────────────────────────────────────────────────────────
    @GetMapping("/payment")
    public String payments(Model model, Authentication auth) {
        User user = getCurrentUser(auth);
        model.addAttribute("currentUser", user);

        // Get all jobs for this staff member
        List<Job> jobs = jobService.findByStaff(user);
        model.addAttribute("jobs", jobs);

        // Fetch real payment record for each job from payments table
        List<Payment> staffPayments = new java.util.ArrayList<>();
        for (Job j : jobs) {
            if (j.getAppointment() != null) {
                paymentService.findByAppointmentId(j.getAppointment().getId())
                        .ifPresent(staffPayments::add);
            }
        }
        model.addAttribute("staffPayments", staffPayments);

        // Count completed jobs
        long completedJobsCount = jobs.stream()
                .filter(j -> j.getStatus() == Job.Status.COMPLETED)
                .count();
        model.addAttribute("completedJobsCount", completedJobsCount);

        // Count PAID payments
        long paidPaymentsCount = staffPayments.stream()
                .filter(p -> p.getStatus() == Payment.Status.PAID)
                .count();
        model.addAttribute("paidPaymentsCount", paidPaymentsCount);

        // Total salary — sum of all PAID payment amounts
        // Total salary — 90% of ALL PAID payments (all time)
        BigDecimal totalSalary = staffPayments.stream()
                .filter(p -> p.getStatus() == Payment.Status.PAID)
                .map(p -> p.getAmount()
                        .multiply(new BigDecimal("0.90")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalSalary", totalSalary);

        // Monthly salary — 90% of PAID payments this month
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        BigDecimal monthlySalary = staffPayments.stream()
                .filter(p -> p.getStatus() == Payment.Status.PAID
                        && p.getPaidAt() != null
                        && !p.getPaidAt().toLocalDate().isBefore(startOfMonth))
                .map(p -> p.getAmount()
                        .multiply(new BigDecimal("0.90")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("monthlySalary", monthlySalary);

        return "staff/payment";
    }

}
