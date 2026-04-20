package com.example.sas.service;

import com.example.sas.dto.AppointmentDto;
import com.example.sas.entity.*;
import com.example.sas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ServiceRepository serviceRepository;
    private final JobRepository jobRepository;

    public Appointment bookAppointment(User customer, AppointmentDto dto) {
        ServiceEntity service = serviceRepository.findById(dto.getServiceId())
                .orElseThrow(() -> new RuntimeException("Service not found"));

        Appointment appointment = new Appointment();
        appointment.setCustomer(customer);
        appointment.setService(service);
        appointment.setAppointmentDate(dto.getAppointmentDate());
        appointment.setAppointmentTime(dto.getAppointmentTime());
        appointment.setNotes(dto.getNotes());
        appointment.setAddress(dto.getAddress());
        appointment.setNumberOfWorkers(dto.getNumberOfWorkers());
        appointment.setStatus(Appointment.Status.PENDING);

        Appointment saved = appointmentRepository.save(appointment);

        // Auto-create a job for the appointment (without worker assignment)
        Job job = new Job();
        job.setAppointment(saved);
        job.setStatus(Job.Status.PENDING);
        jobRepository.save(job);

        return saved;
    }

    public List<Appointment> findByCustomer(User customer) {
        return appointmentRepository.findByCustomerOrderByCreatedAtDesc(customer);
    }

    public List<Appointment> findAll() {
        return appointmentRepository.findAllOrderByCreatedAtDesc();
    }

    public Optional<Appointment> findById(Long id) {
        return appointmentRepository.findById(id);
    }

    public Appointment updateStatus(Long id, Appointment.Status status) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        appointment.setStatus(status);
        return appointmentRepository.save(appointment);
    }

    public void cancel(Long id, User requester, String bankName, String accNo, String holderName, String branch) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        boolean isAdmin = requester.getRole() == User.Role.ADMIN;
        boolean isOwner = appointment.getCustomer().getId().equals(requester.getId());

        if (!isAdmin && !isOwner) {
            throw new RuntimeException("Not authorized to cancel this appointment");
        }

        appointment.setStatus(Appointment.Status.CANCELLED);
        appointment.setRefundBankName(bankName);
        appointment.setRefundAccountNumber(accNo);
        appointment.setRefundAccountHolderName(holderName);
        appointment.setRefundBranchName(branch);
        
        appointmentRepository.save(appointment);
    }

    public long countByStatus(Appointment.Status status) {
        return appointmentRepository.countByStatus(status);
    }
}
