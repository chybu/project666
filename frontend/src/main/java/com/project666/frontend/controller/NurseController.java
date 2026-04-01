package com.project666.frontend.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/nurse")
@PreAuthorize("hasRole('NURSE')")
public class NurseController {

    @GetMapping("/homepage")
    public String redirectHomepage() {
        return "redirect:/nurse/dashboard/home";
    }

    @GetMapping("/dashboard/home")
    public String home() {
        return "nurse/dashboard/home";
    }

    @GetMapping("/dashboard/appointments")
    public String appointments(Model model) {
        // later: inject AppointmentService
        return "nurse/dashboard/appointments";
    }

    @GetMapping("/dashboard/notifications")
    public String notifications() {
        return "nurse/dashboard/notifications";
    }

    @GetMapping("/dashboard/profile")
    public String profile() {
        return "nurse/dashboard/profile";
    }
}