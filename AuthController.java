package com.example.sas.controller;

import com.example.sas.dto.UserRegistrationDto;
import com.example.sas.service.ServiceService;
import com.example.sas.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final ServiceService serviceService; // NEW: to populate service dropdown

    // ─── ROOT REDIRECT ─────────────────────────────────────────────────────
    @GetMapping("/")
    public String root() {
        return "redirect:/auth/login";
    }

    // ─── STAFF / CUSTOMER LOGIN ────────────────────────────────────────────
    @GetMapping("/auth/login")
    public String loginPage(@RequestParam(name = "error", required = false) String error,
                            @RequestParam(name = "logout", required = false) String logout,
                            Model model) {
        if (error != null)  model.addAttribute("error",  "Invalid email or password.");
        if (logout != null) model.addAttribute("logout", "You have been logged out.");
        return "auth/login";
    }

    @GetMapping("/auth/register")
    public String registerPage(Model model) {
        model.addAttribute("dto", new UserRegistrationDto());
        prepareRegistrationModel(model, false);
        return "auth/register";
    }

    @GetMapping("/auth/register/worker")
    public String workerRegisterPage(Model model) {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setRole("STAFF");
        model.addAttribute("dto", dto);
        prepareRegistrationModel(model, true);
        return "auth/register";
    }

    @PostMapping("/auth/register")
    public String register(@Valid @ModelAttribute("dto") UserRegistrationDto dto,
                           @RequestParam(name = "workerOnly", defaultValue = "false") boolean workerOnly,
                           BindingResult result,
                           RedirectAttributes flash,
                           Model model) {
        if (workerOnly) {
            dto.setRole("STAFF");
        }

        if (result.hasErrors()) {
            prepareRegistrationModel(model, workerOnly);
            return "auth/register";
        }
        try {
            userService.registerUser(dto);
            flash.addFlashAttribute("success", "Registration successful! Please log in.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            prepareRegistrationModel(model, workerOnly);
            return "auth/register";
        }
    }

    private void prepareRegistrationModel(Model model, boolean workerOnly) {
        model.addAttribute("workerOnly", workerOnly);
        model.addAttribute("services", serviceService.findActiveServices());
    }

    // ─── ADMIN LOGIN ───────────────────────────────────────────────────────
    @GetMapping("/admin/login")
    public String adminLoginPage(@RequestParam(name = "error", required = false) String error,
                                 @RequestParam(name = "logout", required = false) String logout,
                                 Model model) {
        if (error != null) {
            if ("notadmin".equals(error)) {
                model.addAttribute("error", "Access denied. This portal is for administrators only.");
            } else {
                model.addAttribute("error", "Invalid admin credentials.");
            }
        }
        if (logout != null) model.addAttribute("logout", "Admin logged out successfully.");
        return "auth/admin-login";
    }
}

