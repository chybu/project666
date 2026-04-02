package com.project666.frontend.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/receptionist")
@PreAuthorize("hasRole('RECEPTIONIST')")
public class ReceptionistController {

    @GetMapping("/dashboard/home")
    public String home() {
        return "receptionist/dashboard/home";
    }

    @GetMapping("/dashboard/appointments")
    public String appointments() {
        return "receptionist/dashboard/appointments";
    }

    @GetMapping("/dashboard/notifications")
    public String notifications() {
        return "receptionist/dashboard/notifications";
    }

    @GetMapping("/dashboard/profile")
    public String profile() {
        return "receptionist/dashboard/profile";
    }
}
