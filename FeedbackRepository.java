package com.example.sas.repository;

import com.example.sas.entity.Feedback;
import com.example.sas.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByCustomer(User customer);
    Optional<Feedback> findByAppointmentId(Long appointmentId);
    boolean existsByAppointmentId(Long appointmentId);
    List<Feedback> findByIsHiddenFalseOrderByCreatedAtDesc();

    @Query("SELECT AVG(f.rating) FROM Feedback f")
    Double getAverageRating();
}
