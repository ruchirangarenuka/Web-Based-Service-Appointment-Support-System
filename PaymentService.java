package com.example.sas.service;

import com.example.sas.entity.*;
import com.example.sas.repository.AppointmentRepository;
import com.example.sas.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;
    private final SmsService smsService;



    private static final String UPLOAD_DIR = "uploads/receipts/";

    /**
     * Platform commission rate - 10%.
     * Change this one constant to update the rate across the whole system.
     */
    public static final BigDecimal COMMISSION_RATE = new BigDecimal("10.00");

    // Commission helpers

    /**
     * Calculate the platform's commission from a gross amount.
    * commission = amount * rate / 100, rounded to 2 decimal places.
     */
    public static BigDecimal calculateCommission(BigDecimal amount) {
        return amount
                .multiply(COMMISSION_RATE)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate the staff payout from a gross amount.
    * payout = amount - commission
     */
    public static BigDecimal calculateStaffPayout(BigDecimal amount) {
        return amount.subtract(calculateCommission(amount));
    }

    /**
     * Stamp commission fields onto a payment that is about to be marked PAID.
     * Called internally by processCardPayment() and adminConfirmPayment().
     */
    private void applyCommission(Payment payment) {
        BigDecimal gross      = payment.getAmount();
        BigDecimal commission = calculateCommission(gross);
        BigDecimal payout     = gross.subtract(commission);

        payment.setCommissionRate(COMMISSION_RATE);
        payment.setCommissionAmount(commission);
        payment.setStaffPayout(payout);
    }

    // Existing methods (unchanged logic, commission added where payment is confirmed)

    /** Create a PENDING payment record for an appointment */
    public Payment createPayment(Long appointmentId, Payment.Method method) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        Optional<Payment> existing = paymentRepository.findByAppointmentId(appointmentId);
        if (existing.isPresent()) return existing.get();

        Payment payment = new Payment();
        payment.setAppointment(appointment);
        payment.setCustomer(appointment.getCustomer());
        payment.setAmount(appointment.getService().getPrice());
        payment.setMethod(method);
        payment.setStatus(Payment.Status.PENDING);
        payment.setTransactionId(generateTransactionId());
        payment.setInvoiceNumber(generateInvoiceNumber());

        return paymentRepository.save(payment);
    }

    /** Card payment - instant confirmation, commission calculated immediately */
    public Payment processCardPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        applyCommission(payment);                       // NEW

        payment.setStatus(Payment.Status.PAID);
        payment.setAdminConfirmed(true);
        payment.setPaidAt(LocalDateTime.now());

        Appointment apt = payment.getAppointment();
        apt.setStatus(Appointment.Status.CONFIRMED);
        appointmentRepository.save(apt);

        smsService.sendBookingConfirmationSms(apt);

        return paymentRepository.save(payment);
    }

    /** Bank transfer - upload receipt, await admin confirmation */
    public Payment uploadReceipt(Long paymentId, MultipartFile receipt) throws IOException {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        Path uploadPath = Paths.get(UPLOAD_DIR);
        Files.createDirectories(uploadPath);
        String filename = "receipt_" + paymentId + "_" + System.currentTimeMillis()
                + getExtension(receipt.getOriginalFilename());
        Path filePath = uploadPath.resolve(filename);
        Files.copy(receipt.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        payment.setReceiptImagePath(filename);
        payment.setStatus(Payment.Status.RECEIPT_UPLOADED);
        return paymentRepository.save(payment);
    }

    /** Admin confirms bank transfer receipt - commission calculated at confirmation */
    public Payment adminConfirmPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        applyCommission(payment);                       // NEW

        payment.setStatus(Payment.Status.PAID);
        payment.setAdminConfirmed(true);
        payment.setPaidAt(LocalDateTime.now());

        Appointment apt = payment.getAppointment();
        apt.setStatus(Appointment.Status.CONFIRMED);
        appointmentRepository.save(apt);

        smsService.sendBookingConfirmationSms(apt);

        return paymentRepository.save(payment);
    }

    /** Admin rejects bank transfer receipt */
    public Payment adminRejectPayment(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        payment.setStatus(Payment.Status.FAILED);
        return paymentRepository.save(payment);
    }

    // Revenue summary queries

    public BigDecimal getTotalRevenue() {
        BigDecimal total = paymentRepository.getTotalRevenue();
        return total != null ? total : BigDecimal.ZERO;
    }

    /** Total platform commission earned across all confirmed payments */
    public BigDecimal getTotalCommission() {
        return paymentRepository.findAll().stream()
                .filter(p -> p.getCommissionAmount() != null)
                .map(Payment::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Total staff payouts across all confirmed payments */
    public BigDecimal getTotalStaffPayout() {
        return paymentRepository.findAll().stream()
                .filter(p -> p.getStaffPayout() != null)
                .map(Payment::getStaffPayout)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Standard finders (unchanged)

    public List<Payment> findByCustomer(User customer) {
        return paymentRepository.findByCustomerOrderByCreatedAtDesc(customer);
    }

    public List<Payment> findAll() {
        return paymentRepository.findAll();
    }

    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }

    public Optional<Payment> findByAppointmentId(Long appointmentId) {
        return paymentRepository.findByAppointmentId(appointmentId);
    }

    public List<Payment> findPendingReceipts() {
        return paymentRepository.findByStatus(Payment.Status.RECEIPT_UPLOADED);
    }

    // Private helpers

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateInvoiceNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "INV-" + date + "-" + (1000 + (int)(Math.random() * 9000));
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf("."));
    }
}