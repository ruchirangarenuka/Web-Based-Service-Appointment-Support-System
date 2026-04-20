package com.example.sas.service;

import com.example.sas.entity.Appointment;
import com.example.sas.entity.Job;
import com.example.sas.entity.Payment;
import com.example.sas.entity.SentSms;
import com.example.sas.repository.JobRepository;
import com.example.sas.repository.PaymentRepository;
import com.example.sas.repository.SentSmsRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SmsService {

    private final PaymentRepository paymentRepository;
    private final JobRepository jobRepository;
    private final SentSmsRepository sentSmsRepository;

    @Value("${twilio.account.sid}")
    private String twilioAccountSid;

    @Value("${twilio.auth.token}")
    private String twilioAuthToken;

    @Value("${twilio.from.number}")
    private String twilioFromNumber;

    @PostConstruct
    public void init() {
        try {
            if (twilioAccountSid != null) twilioAccountSid = twilioAccountSid.trim();
            if (twilioAuthToken != null) twilioAuthToken = twilioAuthToken.trim();
            if (twilioFromNumber != null) twilioFromNumber = twilioFromNumber.trim();

            if (twilioAccountSid != null && !twilioAccountSid.startsWith("ACxxxx")) {
                Twilio.init(twilioAccountSid, twilioAuthToken);
                log.info("Twilio SMS Service initialized with account: {}", twilioAccountSid);
            }
        } catch (Exception e) {
            log.error("Failed to initialize Twilio: {}", e.getMessage());
        }
    }

    /**
     * Send confirmation SMS to customer
     */
    public void sendBookingConfirmationSms(Appointment appointment) {
        if (appointment == null || appointment.getCustomer() == null) return;

        String customerName = appointment.getCustomer().getFullName();
        String phoneNumber = appointment.getCustomer().getPhone();
        
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            log.warn("Cannot send SMS: Customer {} has no phone number.", customerName);
            return;
        }

        // 1. Gather Details
        String serviceName = (appointment.getService() != null) ? appointment.getService().getName() : "Service";
        String date = (appointment.getAppointmentDate() != null) ? appointment.getAppointmentDate().toString() : "TBA";
        String time = (appointment.getAppointmentTime() != null) ? appointment.getAppointmentTime().toString() : "TBA";

        // 2. Check for assigned staff
        Optional<Job> jobOpt = jobRepository.findByAppointmentId(appointment.getId());
        String workerName = jobOpt.flatMap(j -> Optional.ofNullable(j.getAssignedStaff()))
                .map(staff -> staff.getFullName())
                .orElse("TBA");

        // 3. Payment Status
        Optional<Payment> paymentOpt = paymentRepository.findByAppointmentId(appointment.getId());
        String amountStr = paymentOpt.map(p -> String.format("%.2f LKR", p.getAmount())).orElse("0.00 LKR");
        String paymentStatus = paymentOpt.map(p -> p.getStatus().name()).orElse("PENDING");

        // 4. Construct Message
        String message = String.format(
            "Fixora: Your booking for '%s' is confirmed! \n" +
            "Date: %s \n" +
            "Time: %s \n" +
            "Worker: %s \n" +
            "Amount: %s (Status: %s) \n" +
            "Thank you for choosing Fixora!",
            serviceName, date, time, workerName, amountStr, paymentStatus
        );

        // 5. Send/Log SMS
        String formattedPhone = formatPhoneNumber(phoneNumber);
        String finalStatus = "SIMULATED";
        try {
            if (twilioAccountSid != null && !twilioAccountSid.startsWith("ACxxxx")) {
                Message.creator(
                    new PhoneNumber(formattedPhone),
                    new PhoneNumber(twilioFromNumber),
                    message
                ).create();
                log.info("Real SMS sent successfully to Twilio for {}", formattedPhone);
                finalStatus = "SENT";
            } else {
                log.info("[SIMULATION] Real credentials missing. Logged for {}:\n{}", formattedPhone, message);
                finalStatus = "SIMULATED";
            }
        } catch (Exception e) {
            log.error("Error sending real SMS through Twilio: {}", e.getMessage());
            finalStatus = "FAILED";
        }
        
        // Always save to DB for History Tracking
        sentSmsRepository.save(new SentSms(phoneNumber, message, finalStatus));
    }

    private String formatPhoneNumber(String phone) {
        if (phone == null) return "";
        phone = phone.replaceAll("\\s+", "");
        if (phone.startsWith("0") && phone.length() == 10) {
            return "+94" + phone.substring(1); // Standard Sri Lankan format
        }
        if (!phone.startsWith("+")) {
            return "+" + phone;
        }
        return phone;
    }
}
