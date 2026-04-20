package com.example.sas.controller;

import com.example.sas.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/feedback")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping
    public String viewFeedbacks(Model model) {
        model.addAttribute("feedbacks", feedbackService.findAll());
        return "admin/feedback";
    }

    @PostMapping("/{id}/hide")
    public String toggleHideFeedback(@PathVariable("id") Long id, RedirectAttributes flash) {
        try {
            feedbackService.hideFeedback(id);
            flash.addFlashAttribute("success", "Feedback visibility updated.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/feedback";
    }

    @PostMapping("/{id}/delete")
    public String deleteFeedback(@PathVariable("id") Long id, RedirectAttributes flash) {
        try {
            feedbackService.deleteFeedbackByAdmin(id);
            flash.addFlashAttribute("success", "Feedback completely deleted.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/feedback";
    }
}
