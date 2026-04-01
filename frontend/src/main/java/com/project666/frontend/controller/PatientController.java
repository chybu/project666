package com.project666.frontend.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.project666.backend.domain.ListAppointmentRequest;
import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.entity.AppointmentStatusEnum;
import com.project666.backend.service.AppointmentService;
import com.project666.frontend.util.OidcUserUtil;
import com.project666.backend.domain.entity.User;

import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
@RequestMapping("/patient")
public class PatientController {

    private final AppointmentService appointmentService;

    @GetMapping("/homepage")
    public String redirectOldHomepage() {
        return "redirect:/patient/dashboard/home";
    }

    @GetMapping("/dashboard/home")
    @PreAuthorize("hasRole('PATIENT')")
    public String loadHomePage(
        @AuthenticationPrincipal OidcUser oidcUser,
        Model model
    ) {
        ListAppointmentRequest request = new ListAppointmentRequest();
        request.setStatus(AppointmentStatusEnum.CONFIRMED);
        request.setFrom(LocalDate.now());
        UUID patientId = OidcUserUtil.getUserId(oidcUser);
        Pageable pageable = PageRequest.of(0, 5, Sort.by("startTime").ascending());
        Page<Appointment> appointmentPage =
            appointmentService.listDoctorAppointment(patientId, request, pageable);

        model.addAttribute("appointments", appointmentPage.getContent());
        return "patient/dashboard/home";
    }

@GetMapping("/dashboard/reviewAppointments")
@PreAuthorize("hasRole('PATIENT')")
public String reviewAppointments(
    @AuthenticationPrincipal OidcUser oidcUser,
    Model model
) {
    ListAppointmentRequest request = new ListAppointmentRequest();
    request.setStatus(AppointmentStatusEnum.CONFIRMED);
    request.setFrom(LocalDate.now());
    UUID patientId = OidcUserUtil.getUserId(oidcUser);
    Pageable pageable = PageRequest.of(0, 10, Sort.by("startTime").ascending());
    Page<Appointment> appointmentPage =
        appointmentService.listDoctorAppointment(patientId, request, pageable);

    model.addAttribute("appointments", appointmentPage.getContent());
    return "patient/dashboard/reviewAppointments";
}

    @GetMapping("/dashboard/finances")
    @PreAuthorize("hasRole('PATIENT')")
    public String finances() {
        return "patient/dashboard/finances";
    }

    @GetMapping("/dashboard/notifications")
    @PreAuthorize("hasRole('PATIENT')")
    public String notifications() {
        return "patient/dashboard/notifications";
    }

    @GetMapping("/dashboard/pharmacy")
    @PreAuthorize("hasRole('PATIENT')")
    public String pharmacy() {
        return "patient/dashboard/pharmacy";
    }

@GetMapping("/dashboard/profile")
@PreAuthorize("hasRole('PATIENT')")
public String profile(){
    return "patient/dashboard/profile";
}

    @GetMapping("/dashboard/security")
    @PreAuthorize("hasRole('PATIENT')")
    public String security() {
        return "patient/dashboard/security";
    }
}