package com.example.sas.repository;

import com.example.sas.entity.SentSms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SentSmsRepository extends JpaRepository<SentSms, Long> {
    List<SentSms> findAllByOrderBySentAtDesc();
}
