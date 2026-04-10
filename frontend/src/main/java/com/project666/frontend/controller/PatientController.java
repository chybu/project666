package com.project666.frontend.controller;

import java.time.LocalDate;
import java.util.UUID;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.project666.backend.domain.ListAppointmentRequest;
import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.entity.AppointmentStatusEnum;
import com.project666.backend.service.AppointmentService;
import com.project666.frontend.service.KeycloakService;
import com.project666.frontend.util.OidcUserUtil;
import com.project666.backend.domain.entity.User;
import com.project666.backend.repository.UserRepository;
import com.project666.backend.domain.CancelAppointmentRequest;
import com.project666.backend.domain.entity.CancellationInitiatorEnum;
import com.project666.backend.domain.entity.RoleEnum;

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
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) LocalDate selectedDate,
            Model model
    ) {
        User user = requireActiveUser(oidcUser);
        UUID patientId = user.getId();

        keycloakService.syncUser(authorizedClient, user);
        userRepository.save(user);

        LocalDate today = LocalDate.now();
        int selectedYear = (year != null) ? year : today.getYear();
        int selectedMonth = (month != null) ? month : today.getMonthValue();

        YearMonth yearMonth = YearMonth.of(selectedYear, selectedMonth);
        LocalDate firstDayOfMonth = yearMonth.atDay(1);
        LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();

        ListAppointmentRequest request = new ListAppointmentRequest();
        request.setStatus(AppointmentStatusEnum.CONFIRMED);
        request.setFrom(firstDayOfMonth);

        Pageable pageable = PageRequest.of(0, 100, Sort.by("startTime").ascending());
        Page<Appointment> appointmentPage =
                appointmentService.listAppointmentForPatient(patientId, request, pageable);

        List<Appointment> monthAppointments = appointmentPage.getContent().stream()
                .filter(a -> !a.getStartTime().toLocalDate().isAfter(lastDayOfMonth))
                .toList();

        Map<LocalDate, List<Appointment>> appointmentsByDate = new HashMap<>();
        for (Appointment appointment : monthAppointments) {
            LocalDate date = appointment.getStartTime().toLocalDate();
            appointmentsByDate
                    .computeIfAbsent(date, k -> new ArrayList<>())
                    .add(appointment);
        }

        List<LocalDate> calendarDays = new ArrayList<>();

        int leadingBlanks = firstDayOfMonth.getDayOfWeek().getValue() % 7;
        for (int i = 0; i < leadingBlanks; i++) {
            calendarDays.add(null);
        }

        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            calendarDays.add(yearMonth.atDay(day));
        }

        while (calendarDays.size() % 7 != 0) {
            calendarDays.add(null);
        }

        YearMonth prevMonth = yearMonth.minusMonths(1);
        YearMonth nextMonth = yearMonth.plusMonths(1);

        LocalDate effectiveSelectedDate = selectedDate;

        List<Appointment> selectedDayAppointments =
                (effectiveSelectedDate != null && appointmentsByDate.containsKey(effectiveSelectedDate))
                        ? appointmentsByDate.get(effectiveSelectedDate)
                        : new ArrayList<>();

        model.addAttribute("appointmentsByDate", appointmentsByDate);
        model.addAttribute("calendarDays", calendarDays);
        model.addAttribute("currentMonthName", yearMonth.getMonth().name());
        model.addAttribute("currentYear", selectedYear);
        model.addAttribute("currentMonth", selectedMonth);
        model.addAttribute("prevYear", prevMonth.getYear());
        model.addAttribute("prevMonth", prevMonth.getMonthValue());
        model.addAttribute("nextYear", nextMonth.getYear());
        model.addAttribute("nextMonth", nextMonth.getMonthValue());
        model.addAttribute("today", today);
        model.addAttribute("selectedDate", effectiveSelectedDate);
        model.addAttribute("selectedDayAppointments", selectedDayAppointments);

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
@PostMapping("/dashboard/cancel-appointment")
public String cancelAppointment(
        @RequestParam UUID appointmentId,
        @AuthenticationPrincipal OidcUser oidcUser
) {
    UUID userId = OidcUserUtil.getUserId(oidcUser);

    CancelAppointmentRequest request = new CancelAppointmentRequest();
    request.setAppointmentId(appointmentId);
    request.setCancelReason("Cancelled by patient from dashboard");
    request.setCancellationInitiator(CancellationInitiatorEnum.PATIENT);

    appointmentService.cancelAppointment(
            userId,
            RoleEnum.PATIENT,
            request
    );

    return "redirect:/patient/dashboard/home";
}
@PostMapping("/profile/update-name")
@PreAuthorize("hasRole('PATIENT')")
public String updateName(
    @AuthenticationPrincipal OidcUser oidcUser,
    @RequestParam String firstName,
    @RequestParam String lastName
){
    UUID userId = OidcUserUtil.getUserId(oidcUser);

    User user = userRepository.findById(userId)
        .orElseThrow();

    user.setFirstName(firstName);
    user.setLastName(lastName);

    userRepository.save(user);

    return "redirect:/patient/dashboard/profile";
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
