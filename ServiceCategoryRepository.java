package com.example.sas.repository;

import com.example.sas.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {
    List<ServiceCategory> findAllByOrderByNameAsc();

    Optional<ServiceCategory> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
