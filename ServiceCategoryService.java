package com.example.sas.service;

import com.example.sas.entity.ServiceCategory;
import com.example.sas.entity.ServiceEntity;
import com.example.sas.repository.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ServiceCategoryService {

    private final ServiceCategoryRepository categoryRepository;
    private final ServiceService serviceService;

    public List<ServiceCategory> findAll() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    public void syncFromExistingServices() {
        List<String> current = categoryRepository.findAllByOrderByNameAsc().stream()
                .map(ServiceCategory::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        for (ServiceEntity service : serviceService.findAllServices()) {
            String cat = normalize(service.getCategory());
            if (cat.isEmpty()) {
                continue;
            }
            if (!current.contains(cat.toLowerCase())) {
                create(cat);
                current.add(cat.toLowerCase());
            }
        }
    }

    public void create(String name) {
        String clean = normalize(name);
        if (clean.isEmpty() || categoryRepository.existsByNameIgnoreCase(clean)) {
            return;
        }
        ServiceCategory category = new ServiceCategory();
        category.setName(clean);
        categoryRepository.save(category);
    }

    public void update(Long id, String name) {
        String clean = normalize(name);
        if (clean.isEmpty()) {
            return;
        }

        ServiceCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getName().equalsIgnoreCase(clean) && categoryRepository.existsByNameIgnoreCase(clean)) {
            return;
        }

        String oldName = category.getName();
        category.setName(clean);
        categoryRepository.save(category);

        // Keep existing services in sync with category rename.
        List<ServiceEntity> services = serviceService.findByCategory(oldName);
        for (ServiceEntity service : services) {
            service.setCategory(clean);
            serviceService.save(service);
        }
    }

    public void delete(Long id) {
        ServiceCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        String deletedName = category.getName();
        categoryRepository.delete(category);

        // Keep service records valid when a category is removed.
        List<ServiceEntity> services = serviceService.findByCategory(deletedName);
        for (ServiceEntity service : services) {
            service.setCategory("Uncategorized");
            serviceService.save(service);
        }
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim();
    }
}
