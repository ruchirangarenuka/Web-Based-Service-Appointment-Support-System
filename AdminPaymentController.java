package com.example.sas.controller;

import com.example.sas.entity.Payment;
import com.example.sas.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentService paymentService;

    // ─── PAYMENTS LIST ────────────────────────────────────────────────────────
    @GetMapping
    public String paymentsPage(Model model) {
        List<Payment> all = paymentService.findAll();

        model.addAttribute("allPayments",     all);
        model.addAttribute("pendingReceipts", paymentService.findPendingReceipts());
        model.addAttribute("totalRevenue",    paymentService.getTotalRevenue());

        // ── Fix: count confirmed payments safely in Java, not SpEL ────────────
        long confirmedCount = all.stream()
                .filter(p -> p.getStatus() == Payment.Status.PAID)
                .count();
        model.addAttribute("confirmedPaymentsCount", confirmedCount);

        // ── Commission summary data ────────────────────────────────────────────
        model.addAttribute("commissionRate",       PaymentService.COMMISSION_RATE);
        model.addAttribute("totalCommission",      paymentService.getTotalCommission());
        model.addAttribute("totalStaffPayout",     paymentService.getTotalStaffPayout());

        return "admin/payments";
    }

    // ─── CONFIRM BANK TRANSFER ────────────────────────────────────────────────
    @PostMapping("/{id}/confirm")
    public String confirmPayment(@PathVariable("id") Long id, RedirectAttributes flash) {
        try {
            paymentService.adminConfirmPayment(id);
            flash.addFlashAttribute("success", "✅ Payment confirmed. Appointment is now CONFIRMED.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/payments";
    }

    // ─── REJECT BANK TRANSFER ─────────────────────────────────────────────────
    @PostMapping("/{id}/reject")
    public String rejectPayment(@PathVariable("id") Long id,
                                @RequestParam(name = "reason", defaultValue = "Receipt rejected by admin") String reason,
                                RedirectAttributes flash) {
        try {
            paymentService.adminRejectPayment(id, reason);
            flash.addFlashAttribute("success", "Receipt rejected. Customer has been notified.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/payments";
    }

    // ─── VIEW INVOICE ─────────────────────────────────────────────────────────
    @GetMapping("/{id}/invoice")
    public String viewInvoice(@PathVariable("id") Long id, Model model) {
        Payment payment = paymentService.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        model.addAttribute("payment", payment);
        return "admin/invoice-view";
    }

    // ─── SERVE RECEIPT IMAGE ──────────────────────────────────────────────────
    @GetMapping("/receipt/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveReceipt(@PathVariable("filename") String filename) {
        try {
            Path filePath = Paths.get("uploads/receipts/").resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                String contentType = filename.toLowerCase().endsWith(".pdf")
                        ? MediaType.APPLICATION_PDF_VALUE
                        : filename.toLowerCase().endsWith(".png")
                        ? MediaType.IMAGE_PNG_VALUE
                        : MediaType.IMAGE_JPEG_VALUE;
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            }
        } catch (Exception ignored) {}
        return ResponseEntity.notFound().build();
    }
}