package com.example.sas.repository;

import com.example.sas.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
    List<ServiceEntity> findByActiveTrue();
    List<ServiceEntity> findByCategory(String category);
    List<ServiceEntity> findByNameContainingIgnoreCase(String name);
}
