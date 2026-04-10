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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.project666.backend.domain.ListAppointmentRequest;
import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.entity.AppointmentStatusEnum;
import com.project666.backend.service.AppointmentService;
import com.project666.frontend.service.KeycloakService;
import com.project666.frontend.util.OidcUserUtil;
import com.project666.backend.domain.entity.User;
import com.project666.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;

@Controller
@RequiredArgsConstructor
@RequestMapping("/patient")
@PreAuthorize("hasRole('PATIENT')")
public class PatientController {

    private final AppointmentService appointmentService;
    private final UserRepository userRepository;
    private final KeycloakService keycloakService;


    @GetMapping("/dashboard/home")
    public String loadHomePage(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
        Model model
    ) {
        User user = requireActiveUser(oidcUser);
        UUID patientId = user.getId();

        keycloakService.syncUser(authorizedClient, user);
        userRepository.save(user);

        ListAppointmentRequest request = new ListAppointmentRequest();
        request.setStatus(AppointmentStatusEnum.CONFIRMED);
        request.setFrom(LocalDate.now());
        Pageable pageable = PageRequest.of(0, 5, Sort.by("startTime").ascending());
        Page<Appointment> appointmentPage =
            appointmentService.listAppointmentForPatient(patientId, request, pageable);

        model.addAttribute("appointments", appointmentPage.getContent());
        return "patient/dashboard/home";
    }

    @GetMapping("/dashboard/reviewAppointments")
    public String reviewAppointments(
        @AuthenticationPrincipal OidcUser oidcUser,
        Model model
    ) {
        User user = requireActiveUser(oidcUser);
        ListAppointmentRequest request = new ListAppointmentRequest();
        request.setStatus(AppointmentStatusEnum.CONFIRMED);
        request.setFrom(LocalDate.now());
        UUID patientId = user.getId();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("startTime").ascending());
        Page<Appointment> appointmentPage =
            appointmentService.listAppointmentForPatient(patientId, request, pageable);

        model.addAttribute("appointments", appointmentPage.getContent());
        return "patient/dashboard/reviewAppointments";
    }

    @GetMapping("/dashboard/finances")
    public String finances() {
        return "patient/dashboard/finances";
    }

    @GetMapping("/dashboard/notifications")
    public String notifications() {
        return "patient/dashboard/notifications";
    }

    @GetMapping("/dashboard/pharmacy")
    public String pharmacy() {
        return "patient/dashboard/pharmacy";
    }

@GetMapping("/dashboard/profile")
@PreAuthorize("hasRole('PATIENT')")
public String profile(
    @AuthenticationPrincipal OidcUser oidcUser,
    @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
    Model model
){
    User user = requireActiveUser(oidcUser);

    keycloakService.syncUser(authorizedClient, user);
    userRepository.save(user);

    model.addAttribute("user", user);

    return "patient/dashboard/profile";
}

@PostMapping("/delete-account")
@PreAuthorize("hasRole('PATIENT')")
public String deleteAccount(
    @AuthenticationPrincipal OidcUser oidcUser
){
    User user = requireActiveUser(oidcUser);

    keycloakService.deleteUser(user.getId());
    user.setDeleted(true);
    userRepository.save(user);

    return "redirect:/logout";
}

@GetMapping("/profile/keycloak")
@PreAuthorize("hasRole('PATIENT')")
public String redirectToKeycloakAccount() {
    return keycloakService.getAccountRedirect();
}

@GetMapping("/profile/keycloak-password")
@PreAuthorize("hasRole('PATIENT')")
public String redirectToKeycloakPassword() {
    return keycloakService.getPasswordRedirect();
}

    @GetMapping("/dashboard/security")
    public String security() {
        return "patient/dashboard/security";
    }

    private User requireActiveUser(OidcUser oidcUser) {
        UUID userId = OidcUserUtil.getUserId(oidcUser);
        return userRepository.findByIdAndDeletedFalse(userId)
            .orElseThrow();
    }

}
