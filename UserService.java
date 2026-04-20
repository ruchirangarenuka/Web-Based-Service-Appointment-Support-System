package com.example.sas.service;

import com.example.sas.dto.UserRegistrationDto;
import com.example.sas.dto.UserProfileUpdateDto;
import com.example.sas.entity.User;
import com.example.sas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(UserRegistrationDto dto) throws IOException {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already registered: " + dto.getEmail());
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        if (dto.getPhone() == null || !dto.getPhone().matches("^\\+?[0-9]{7,15}$")) {
            throw new RuntimeException("Invalid phone number format");
        }

        if (dto.getRole() != null && "ADMIN".equalsIgnoreCase(dto.getRole())) {
            throw new RuntimeException("Admin accounts are managed separately.");
        }

        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setRole(User.Role.valueOf(dto.getRole().toUpperCase()));
        user.setEnabled(true);

        // Handle CUSTOMER-specific fields
        if ("CUSTOMER".equalsIgnoreCase(dto.getRole())) {
            user.setAddress(dto.getAddress());
            user.setCity(dto.getCity());
        }

        // Handle STAFF-specific fields
        if ("STAFF".equalsIgnoreCase(dto.getRole())) {
            if (dto.getServiceType() == null || dto.getServiceType().isBlank()) {
                throw new RuntimeException("Service type is required for staff registration");
            }
            if (dto.getAvailability() == null || dto.getAvailability().length == 0) {
                throw new RuntimeException("Please select at least one availability day");
            }
            if (dto.getAvailableFromTime() == null || dto.getAvailableToTime() == null) {
                throw new RuntimeException("Please select an availability time range");
            }
            if (!dto.getAvailableFromTime().isBefore(dto.getAvailableToTime())) {
                throw new RuntimeException("Availability start time must be before end time");
            }

            MultipartFile idPhoto = dto.getIdPhoto();
            if (idPhoto == null || idPhoto.isEmpty()) {
                throw new RuntimeException("ID document photo is required for worker registration");
            }

            // Save staff-specific fields
            user.setServiceType(dto.getServiceType());
            user.setAvailability(Arrays.stream(dto.getAvailability())
                    .filter(day -> day != null && !day.isBlank())
                    .collect(Collectors.joining(", ")));
            user.setAvailableFromTime(dto.getAvailableFromTime());
            user.setAvailableToTime(dto.getAvailableToTime());
            user.setYearsOfExperience(dto.getYearsOfExperience());
            user.setQualifications(dto.getQualifications());
            user.setEmergencyContactName(dto.getEmergencyContactName());
            user.setEmergencyContactPhone(dto.getEmergencyContactPhone());
            user.setHourlyRate(dto.getHourlyRate());

            // Save ID photo to disk
            String uploadDirPath = System.getProperty("user.dir") + "/uploads/id-photos";
            Path uploadDir = Paths.get(uploadDirPath);
            Files.createDirectories(uploadDir);

            String filename = System.currentTimeMillis() + "_" + idPhoto.getOriginalFilename();
            Path filePath = uploadDir.resolve(filename);
            idPhoto.transferTo(filePath.toFile());

            user.setIdPhotoUrl("/uploads/id-photos/" + filename);
        }

        // Handle ADMIN-specific fields
        if ("ADMIN".equalsIgnoreCase(dto.getRole())) {
            if (dto.getEmployeeId() == null || dto.getEmployeeId().isBlank()) {
                throw new RuntimeException("Employee ID is required for admin registration");
            }
            if (dto.getDepartment() == null || dto.getDepartment().isBlank()) {
                throw new RuntimeException("Department is required for admin registration");
            }
            if (dto.getAccessLevel() == null || dto.getAccessLevel().isBlank()) {
                throw new RuntimeException("Access level is required for admin registration");
            }
            if (dto.getSecurityQuestion() == null || dto.getSecurityQuestion().isBlank()) {
                throw new RuntimeException("Security question is required for admin registration");
            }
            if (dto.getSecurityAnswer() == null || dto.getSecurityAnswer().isBlank()) {
                throw new RuntimeException("Security answer is required for admin registration");
            }

            user.setEmployeeId(dto.getEmployeeId());
            user.setDepartment(dto.getDepartment());
            user.setAccessLevel(dto.getAccessLevel());
            user.setSecurityQuestion(dto.getSecurityQuestion());
            user.setSecurityAnswer(dto.getSecurityAnswer());
        }

        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public List<User> findByRole(User.Role role) {
        return userRepository.findByRole(role);
    }

    public User updateProfile(Long userId, UserProfileUpdateDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        MultipartFile profilePicture = dto.getProfilePicture();
        if (profilePicture != null && !profilePicture.isEmpty()) {
            saveProfilePicture(user, profilePicture);
        }

        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setAddress(dto.getAddress());

        if (dto.getNewPassword() != null && !dto.getNewPassword().isBlank()) {
            if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
                throw new RuntimeException("Current password is incorrect");
            }
            if (!dto.getNewPassword().equals(dto.getConfirmNewPassword())) {
                throw new RuntimeException("New passwords do not match");
            }
            user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        }

        // ── STAFF EXTENSIONS ──
        if (user.getRole() == User.Role.STAFF) {
            if (dto.getAvailability() != null) {
                user.setAvailability(String.join(", ", dto.getAvailability()));
            } else {
                user.setAvailability(""); 
            }
            if (dto.getAvailableFromTime() != null) {
                user.setAvailableFromTime(dto.getAvailableFromTime());
            }
            if (dto.getAvailableToTime() != null) {
                user.setAvailableToTime(dto.getAvailableToTime());
            }
        }

        return userRepository.save(user);
    }

    private void saveProfilePicture(User user, MultipartFile profilePicture) {
        String contentType = profilePicture.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Profile picture must be an image file");
        }

        String originalName = profilePicture.getOriginalFilename();
        String extension = ".jpg";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf('.'));
        }

        String filename = "profile_" + user.getId() + "_" + System.currentTimeMillis() + extension;
        Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "profile-pictures");

        try {
            Files.createDirectories(uploadDir);
            Path filePath = uploadDir.resolve(filename);
            profilePicture.transferTo(filePath.toFile());
            user.setProfilePictureUrl("/uploads/profile-pictures/" + filename);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload profile picture", e);
        }
    }

    public User toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setEnabled(!user.isEnabled());
        return userRepository.save(user);
    }

    /**
     * Find available workers for a specific service and time slot
     */
    public List<User> findAvailableWorkers(String serviceType, LocalDate date, LocalTime time) {
        // Get all enabled staff workers
        List<User> staffWorkers = userRepository.findByRoleAndEnabled(User.Role.STAFF, true);
        
        return staffWorkers.stream()
                .filter(worker -> isWorkerAvailable(worker, serviceType, date, time))
                .collect(Collectors.toList());
    }

    /**
     * Check if a worker is available for a specific service and time slot
     */
    private boolean isWorkerAvailable(User worker, String serviceType, LocalDate date, LocalTime time) {
        // Check if worker provides the required service type
        if (worker.getServiceType() == null || !worker.getServiceType().equalsIgnoreCase(serviceType)) {
            return false;
        }

        // Check day availability (availability is stored as comma-separated days)
        if (worker.getAvailability() != null) {
            String dayOfWeek = date.getDayOfWeek().toString().toLowerCase();
            String[] availableDays = worker.getAvailability().toLowerCase().split(",");
            boolean isDayAvailable = Arrays.stream(availableDays)
                    .anyMatch(day -> day.trim().equals(dayOfWeek));
            
            if (!isDayAvailable) {
                return false;
            }
        }

        // Check time availability
        if (worker.getAvailableFromTime() != null && worker.getAvailableToTime() != null) {
            // Calculate appointment end time (assuming 1 hour duration for now)
            LocalTime appointmentEndTime = time.plusHours(1);
            
            // Check if the appointment time slot falls within worker's available hours
            boolean timeAvailable = !time.isBefore(worker.getAvailableFromTime()) && 
                                   !appointmentEndTime.isAfter(worker.getAvailableToTime());
            
            if (!timeAvailable) {
                return false;
            }
        }

        return true;
    }

    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public long countByRole(User.Role role) {
        return userRepository.countByRole(role);
    }
}
