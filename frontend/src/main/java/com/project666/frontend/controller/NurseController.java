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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.project666.backend.domain.ListAppointmentRequest;
import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.entity.AppointmentStatusEnum;
import com.project666.backend.domain.entity.RoleEnum;
import com.project666.backend.service.AppointmentService;
import com.project666.frontend.util.OidcUserUtil;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/nurse")
@PreAuthorize("hasRole('NURSE')")
@RequiredArgsConstructor
public class NurseController {

    private final AppointmentService appointmentService;

    @GetMapping("/dashboard/home")
    public String home() {
        return "nurse/dashboard/home";
    }

    @GetMapping("/dashboard/appointments")
    public String appointments(
        @AuthenticationPrincipal OidcUser oidcUser,
        Model model
    ) {
        UUID nurseId = OidcUserUtil.getUserId(oidcUser);

        Pageable pageable = PageRequest.of(0, 20, Sort.by("startTime").ascending());

        ListAppointmentRequest request = new ListAppointmentRequest();
        request.setStatus(AppointmentStatusEnum.CONFIRMED);
        request.setFrom(LocalDate.now());

        Page<Appointment> appointmentPage =
            appointmentService.listAppointment(request, pageable);

        model.addAttribute("appointments", appointmentPage.getContent());
        return "nurse/dashboard/appointments";
    }

    @PostMapping("/dashboard/appointments/{appointmentId}/no-show")
    public String markNoShow(
        @AuthenticationPrincipal OidcUser oidcUser,
        @PathVariable UUID appointmentId
    ) {
        UUID nurseId = OidcUserUtil.getUserId(oidcUser);
        appointmentService.noShowAppointment(nurseId, appointmentId);
        return "redirect:/nurse/dashboard/appointments";
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