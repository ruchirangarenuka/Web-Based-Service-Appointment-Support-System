package com.example.sas.service;

import com.example.sas.entity.Appointment;
import com.example.sas.entity.Feedback;
import com.example.sas.entity.User;
import com.example.sas.repository.AppointmentRepository;
import com.example.sas.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final AppointmentRepository appointmentRepository;

    public Feedback submitFeedback(User customer, Long appointmentId, int rating, String comment) {
        if (feedbackRepository.existsByAppointmentId(appointmentId)) {
            throw new RuntimeException("Feedback already submitted for this appointment");
        }
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        if (!appointment.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("Not authorized");
        }
        Feedback feedback = new Feedback();
        feedback.setCustomer(customer);
        feedback.setAppointment(appointment);
        feedback.setRating(rating);
        feedback.setComment(comment);
        return feedbackRepository.save(feedback);
    }

    public boolean existsByAppointmentId(Long appointmentId) {
        return feedbackRepository.existsByAppointmentId(appointmentId);
    }

    public java.util.Optional<Feedback> findByAppointmentId(Long appointmentId) {
        return feedbackRepository.findByAppointmentId(appointmentId);
    }

    public List<Feedback> findAll() {
        return feedbackRepository.findAll();
    }

    public List<Feedback> findByCustomer(User customer) {
        return feedbackRepository.findByCustomer(customer);
    }

    public Double getAverageRating() {
        Double avg = feedbackRepository.getAverageRating();
        return avg != null ? avg : 0.0;
    }

    public List<Feedback> getPublicFeedbacks() {
        return feedbackRepository.findByIsHiddenFalseOrderByCreatedAtDesc();
    }

    public void updateFeedback(Long id, User customer, int rating, String comment) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));
        if (!feedback.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("Not authorized to edit this feedback");
        }
        feedback.setRating(rating);
        feedback.setComment(comment);
        feedbackRepository.save(feedback);
    }

    public void deleteFeedback(Long id, User customer) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));
        if (!feedback.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("Not authorized to delete this feedback");
        }
        feedbackRepository.delete(feedback);
    }

    public void hideFeedback(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));
        feedback.setIsHidden(!feedback.getIsHidden());
        feedbackRepository.save(feedback);
    }

    public void deleteFeedbackByAdmin(Long id) {
        feedbackRepository.deleteById(id);
    }
}
