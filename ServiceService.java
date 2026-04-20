package com.example.sas.service;

import com.example.sas.entity.ServiceEntity;
import com.example.sas.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ServiceService {

    private final ServiceRepository serviceRepository;

    public List<ServiceEntity> findAllServices() {
        return serviceRepository.findAll();
    }

    public List<ServiceEntity> findActiveServices() {
        return serviceRepository.findByActiveTrue();
    }

    public Optional<ServiceEntity> findById(Long id) {
        return serviceRepository.findById(id);
    }

    public ServiceEntity save(ServiceEntity service) {
        return serviceRepository.save(service);
    }

    public ServiceEntity toggleStatus(Long id) {
        ServiceEntity service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        service.setActive(!service.isActive());
        return serviceRepository.save(service);
    }

    public void delete(Long id) {
        serviceRepository.deleteById(id);
    }

    public List<ServiceEntity> findByCategory(String category) {
        return serviceRepository.findByCategory(category);
    }
}
