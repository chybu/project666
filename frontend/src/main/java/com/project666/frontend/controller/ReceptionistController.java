package com.project666.frontend.controller;

import java.util.UUID;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;

import com.project666.backend.domain.ListAppointmentRequest;
import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.entity.AppointmentStatusEnum;
import com.project666.backend.service.AppointmentService;
import com.project666.backend.domain.entity.CancellationInitiatorEnum;
import com.project666.backend.exception.TimeNotInWorkingHourException;
import com.project666.backend.exception.InvalidCreateAppointmentTimeWindowException;
import com.project666.backend.exception.OverlapAppointmentException;


import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.project666.frontend.service.KeycloakService;
import com.project666.frontend.util.OidcUserUtil;
import com.project666.backend.domain.entity.User;
import com.project666.backend.repository.UserRepository;
import com.project666.backend.domain.CancelAppointmentRequest;
import com.project666.backend.domain.entity.RoleEnum;
import com.project666.backend.domain.CreateAppointmentRequest;
import com.project666.backend.domain.entity.AppointmentTypeEnum;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/receptionist")
@PreAuthorize("hasRole('RECEPTIONIST')")
public class ReceptionistController {

    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final AppointmentService appointmentService;


    @GetMapping("/dashboard/home")
    public String home(
            @AuthenticationPrincipal OidcUser oidcUser,
            @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) LocalDate selectedDate,
            Model model
    ) {
        User user = requireActiveUser(oidcUser);
        UUID userId = user.getId();

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

        Pageable pageable = PageRequest.of(0, 200, Sort.by("startTime").ascending());
        Page<Appointment> appointmentPage =
                appointmentService.searchAnyAppointmentForReceptionist(userId, request, pageable);

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

        return "receptionist/dashboard/home";
    }

    @GetMapping("/dashboard/appointments")
    public String appointments(
            @AuthenticationPrincipal OidcUser oidcUser,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String doctorId,
            @RequestParam(required = false) AppointmentStatusEnum status,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate end,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        User user = requireActiveUser(oidcUser);
        UUID receptionistId = user.getId();

        UUID parsedPatientId = null;
        UUID parsedDoctorId = null;

        String patientIdError = null;
        String doctorIdError = null;

        if (patientId != null && !patientId.isBlank()) {
            try {
                parsedPatientId = UUID.fromString(patientId.trim());
            } catch (IllegalArgumentException e) {
                patientIdError = "Patient ID must be a valid UUID.";
            }
        }

        if (doctorId != null && !doctorId.isBlank()) {
            try {
                parsedDoctorId = UUID.fromString(doctorId.trim());
            } catch (IllegalArgumentException e) {
                doctorIdError = "Doctor ID must be a valid UUID.";
            }
        }

        Page<Appointment> appointmentPage = Page.empty();
        List<Appointment> appointments = new ArrayList<>();

        if (patientIdError == null && doctorIdError == null) {
            ListAppointmentRequest request = new ListAppointmentRequest();
            request.setPatientId(parsedPatientId);
            request.setDoctorId(parsedDoctorId);
            request.setStatus(status);
            request.setFrom(from);
            request.setEnd(end);

            Pageable pageable = PageRequest.of(page, 20, Sort.by("startTime").ascending());

            appointmentPage = appointmentService.searchAnyAppointmentForReceptionist(
                    receptionistId,
                    request,
                    pageable
            );

            appointments = appointmentPage.getContent();
        }

        model.addAttribute("appointmentPage", appointmentPage);
        model.addAttribute("appointments", appointments);

        model.addAttribute("patientId", patientId);
        model.addAttribute("doctorId", doctorId);
        model.addAttribute("status", status);
        model.addAttribute("from", from);
        model.addAttribute("end", end);
        model.addAttribute("currentPage", page);

        model.addAttribute("patientIdError", patientIdError);
        model.addAttribute("doctorIdError", doctorIdError);
        model.addAttribute("now", java.time.LocalDateTime.now());

        addUserSelectors(model);
        return "receptionist/dashboard/appointments";
    }

    @GetMapping("/dashboard/my-confirmed-appointments")
    public String myConfirmedAppointments(
            @AuthenticationPrincipal OidcUser oidcUser,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String doctorId,
            @RequestParam(required = false) AppointmentTypeEnum type,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate end,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        User user = requireActiveUser(oidcUser);
        UUID receptionistId = user.getId();

        UUID parsedPatientId = null;
        UUID parsedDoctorId = null;

        String patientIdError = null;
        String doctorIdError = null;

        if (patientId != null && !patientId.isBlank()) {
            try {
                parsedPatientId = UUID.fromString(patientId.trim());
            } catch (IllegalArgumentException e) {
                patientIdError = "Patient UUID must be a valid UUID.";
            }
        }

        if (doctorId != null && !doctorId.isBlank()) {
            try {
                parsedDoctorId = UUID.fromString(doctorId.trim());
            } catch (IllegalArgumentException e) {
                doctorIdError = "Doctor UUID must be a valid UUID.";
            }
        }

        Page appointmentPage = Page.empty();
        List appointments = new ArrayList();

        if (patientIdError == null && doctorIdError == null) {
            ListAppointmentRequest request = new ListAppointmentRequest();
            request.setPatientId(parsedPatientId);
            request.setDoctorId(parsedDoctorId);
            request.setType(type);
            request.setFrom(from);
            request.setEnd(end);

            Pageable pageable = PageRequest.of(page, 20, Sort.by("confirmedAt").descending());

            appointmentPage = appointmentService.listAppointmentForReceptionist(
                    receptionistId,
                    request,
                    pageable
            );

            appointments = appointmentPage.getContent();
        }

        model.addAttribute("appointmentPage", appointmentPage);
        model.addAttribute("appointments", appointments);

        model.addAttribute("patientId", patientId);
        model.addAttribute("doctorId", doctorId);
        model.addAttribute("type", type);
        model.addAttribute("from", from);
        model.addAttribute("end", end);
        model.addAttribute("currentPage", page);

        model.addAttribute("patientIdError", patientIdError);
        model.addAttribute("doctorIdError", doctorIdError);

        addUserSelectors(model);
        return "receptionist/dashboard/my-confirmed-appointments";
    }

    @GetMapping("/dashboard/check-in")
    public String checkInPage(
            @AuthenticationPrincipal OidcUser oidcUser,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String doctorId,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        User user = requireActiveUser(oidcUser);
        UUID receptionistId = user.getId();

        UUID parsedPatientId = null;
        UUID parsedDoctorId = null;

        String patientIdError = null;
        String doctorIdError = null;

        if (patientId != null && !patientId.isBlank()) {
            try {
                parsedPatientId = UUID.fromString(patientId.trim());
            } catch (IllegalArgumentException e) {
                patientIdError = "Patient UUID must be a valid UUID.";
            }
        }

        if (doctorId != null && !doctorId.isBlank()) {
            try {
                parsedDoctorId = UUID.fromString(doctorId.trim());
            } catch (IllegalArgumentException e) {
                doctorIdError = "Doctor UUID must be a valid UUID.";
            }
        }

        Page<Appointment> appointmentPage = Page.empty();
        List<Appointment> appointments = new ArrayList<>();

        if (patientIdError == null && doctorIdError == null) {
            ListAppointmentRequest request = new ListAppointmentRequest();
            request.setPatientId(parsedPatientId);
            request.setDoctorId(parsedDoctorId);
            request.setStatus(AppointmentStatusEnum.CONFIRMED);
            request.setFrom(LocalDate.now());
            request.setEnd(LocalDate.now());

            Pageable pageable = PageRequest.of(page, 20, Sort.by("startTime").ascending());

            appointmentPage = appointmentService.searchAnyAppointmentForReceptionist(
                    receptionistId,
                    request,
                    pageable
            );

            appointments = appointmentPage.getContent();
        }

        model.addAttribute("appointmentPage", appointmentPage);
        model.addAttribute("appointments", appointments);
        model.addAttribute("patientId", patientId);
        model.addAttribute("doctorId", doctorId);
        model.addAttribute("currentPage", page);
        model.addAttribute("patientIdError", patientIdError);
        model.addAttribute("doctorIdError", doctorIdError);
        model.addAttribute("now", LocalDateTime.now());

        addUserSelectors(model);
        return "receptionist/dashboard/check-in";
    }

    @GetMapping("/dashboard/notifications")
    public String notifications() {
        return "receptionist/dashboard/notifications";
    }

    @GetMapping("/dashboard/profile")
    public String profile(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
        Model model
    ){
        User user = requireActiveUser(oidcUser);

        keycloakService.syncUser(authorizedClient, user);
        userRepository.save(user);

        model.addAttribute("user", user);

        return "receptionist/dashboard/profile";
    }

    @GetMapping("/dashboard/create-appointment")
    public String createAppointmentPage(Model model) {
        addUserSelectors(model);
        return "receptionist/dashboard/create-appointment";
    }


    @PostMapping("/dashboard/create-appointment")
    public String createAppointment(
            @AuthenticationPrincipal OidcUser oidcUser,
            @RequestParam String patientId,
            @RequestParam String doctorId,
            @RequestParam AppointmentTypeEnum type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            Model model
    ) {
        User user = requireActiveUser(oidcUser);
        UUID receptionistId = user.getId();

        String patientIdError = null;
        String doctorIdError = null;
        String generalError = null;

        UUID parsedPatientId = null;
        UUID parsedDoctorId = null;

        if (patientId != null && !patientId.isBlank()) {
            try {
                parsedPatientId = UUID.fromString(patientId.trim());
            } catch (IllegalArgumentException e) {
                patientIdError = "Please enter a valid patient UUID.";
            }
        } else {
            patientIdError = "Patient UUID is required.";
        }

        if (doctorId != null && !doctorId.isBlank()) {
            try {
                parsedDoctorId = UUID.fromString(doctorId.trim());
            } catch (IllegalArgumentException e) {
                doctorIdError = "Please enter a valid doctor UUID.";
            }
        } else {
            doctorIdError = "Doctor UUID is required.";
        }

        if (patientIdError != null || doctorIdError != null) {
            model.addAttribute("patientId", patientId);
            model.addAttribute("doctorId", doctorId);
            model.addAttribute("type", type);
            model.addAttribute("startTime", startTime);
            model.addAttribute("patientIdError", patientIdError);
            model.addAttribute("doctorIdError", doctorIdError);
            addUserSelectors(model);
            return "receptionist/dashboard/create-appointment";
        }

        try {
            CreateAppointmentRequest request = new CreateAppointmentRequest();
            request.setPatientId(parsedPatientId);
            request.setDoctorId(parsedDoctorId);
            request.setType(type);
            request.setStartTime(startTime);

            appointmentService.createAppointmentForReceptionist(receptionistId, request);

            return "redirect:/receptionist/dashboard/appointments";
        } catch (Exception e) {

            generalError = null;

            if (e instanceof TimeNotInWorkingHourException) {
                generalError = "Appointment time must be between 8:00 AM and 6:00 PM.";

            } else if (e instanceof InvalidCreateAppointmentTimeWindowException) {
                generalError = e.getMessage();

            } else if (e instanceof OverlapAppointmentException) {
                generalError = "This appointment overlaps with another appointment.";

            } else if (e instanceof NoSuchElementException) {
                String msg = e.getMessage();

                if (msg != null && msg.contains("DOCTOR")) {
                    generalError = "Doctor not found. Please check the UUID.";
                } else if (msg != null && msg.contains("PATIENT")) {
                    generalError = "Patient not found. Please check the UUID.";
                } else {
                    generalError = msg;
                }

            } else if (e instanceof IllegalArgumentException) {
                generalError = e.getMessage();

            } else {
                generalError = "Unexpected error occurred while creating the appointment.";
            }

            model.addAttribute("patientId", patientId);
            model.addAttribute("doctorId", doctorId);
            model.addAttribute("type", type);
            model.addAttribute("startTime", startTime);
            model.addAttribute("patientIdError", patientIdError);
            model.addAttribute("doctorIdError", doctorIdError);
            model.addAttribute("generalError", generalError);
            addUserSelectors(model);
            return "receptionist/dashboard/create-appointment";
        }
    }
    @PostMapping("/dashboard/check-in")
    public String checkInAppointment(
            @RequestParam UUID appointmentId,
            @RequestParam(required = false) String sourcePage,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String doctorId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal OidcUser oidcUser
    ) {
        User user = requireActiveUser(oidcUser);
        UUID receptionistId = user.getId();

        appointmentService.confirmAppointment(receptionistId, appointmentId);

        if ("check-in".equals(sourcePage)) {
            StringBuilder redirectUrl = new StringBuilder("redirect:/receptionist/dashboard/check-in?page=" + page);

            if (patientId != null && !patientId.isBlank()) {
                redirectUrl.append("&patientId=").append(patientId);
            }
            if (doctorId != null && !doctorId.isBlank()) {
                redirectUrl.append("&doctorId=").append(doctorId);
            }

            return redirectUrl.toString();
        }

        StringBuilder redirectUrl = new StringBuilder("redirect:/receptionist/dashboard/appointments?page=" + page);

        if (patientId != null && !patientId.isBlank()) {
            redirectUrl.append("&patientId=").append(patientId);
        }
        if (doctorId != null && !doctorId.isBlank()) {
            redirectUrl.append("&doctorId=").append(doctorId);
        }
        if (status != null && !status.isBlank()) {
            redirectUrl.append("&status=").append(status);
        }
        if (from != null && !from.isBlank()) {
            redirectUrl.append("&from=").append(from);
        }
        if (end != null && !end.isBlank()) {
            redirectUrl.append("&end=").append(end);
        }

        return redirectUrl.toString();
    }

    @PostMapping("/dashboard/cancel-appointment")
    public String cancelAppointment(
            @RequestParam UUID appointmentId,
            @RequestParam String cancelReason,
            @RequestParam CancellationInitiatorEnum cancellationInitiator,
            @RequestParam(required = false) String sourcePage,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String doctorId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String selectedDate,
            @AuthenticationPrincipal OidcUser oidcUser
    ) {
        User user = requireActiveUser(oidcUser);
        UUID userId = user.getId();

        CancelAppointmentRequest request = new CancelAppointmentRequest();
        request.setAppointmentId(appointmentId);
        request.setCancelReason(cancelReason);
        request.setCancellationInitiator(cancellationInitiator);

        appointmentService.cancelAppointment(userId, RoleEnum.RECEPTIONIST, request);

        if ("home".equals(sourcePage)) {
            StringBuilder redirectUrl = new StringBuilder("redirect:/receptionist/dashboard/home");

            boolean hasQuery = false;

            if (year != null) {
                redirectUrl.append(hasQuery ? "&" : "?").append("year=").append(year);
                hasQuery = true;
            }
            if (month != null) {
                redirectUrl.append(hasQuery ? "&" : "?").append("month=").append(month);
                hasQuery = true;
            }
            if (selectedDate != null && !selectedDate.isBlank()) {
                redirectUrl.append(hasQuery ? "&" : "?").append("selectedDate=").append(selectedDate);
            }

            return redirectUrl.toString();
        }

        StringBuilder redirectUrl = new StringBuilder("redirect:/receptionist/dashboard/appointments?page=" + page);

        if (patientId != null && !patientId.isBlank()) {
            redirectUrl.append("&patientId=").append(patientId);
        }
        if (doctorId != null && !doctorId.isBlank()) {
            redirectUrl.append("&doctorId=").append(doctorId);
        }
        if (status != null && !status.isBlank()) {
            redirectUrl.append("&status=").append(status);
        }
        if (from != null && !from.isBlank()) {
            redirectUrl.append("&from=").append(from);
        }
        if (end != null && !end.isBlank()) {
            redirectUrl.append("&end=").append(end);
        }

        return redirectUrl.toString();
    }

    @PostMapping("/dashboard/no-show")
    public String noShowAppointment(
            @RequestParam UUID appointmentId,
            @RequestParam(required = false) String sourcePage,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String doctorId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal OidcUser oidcUser
    ) {
        User user = requireActiveUser(oidcUser);
        UUID receptionistId = user.getId();

        appointmentService.noShowAppointment(receptionistId, appointmentId);

        if ("check-in".equals(sourcePage)) {
            StringBuilder redirectUrl = new StringBuilder("redirect:/receptionist/dashboard/check-in?page=" + page);

            if (patientId != null && !patientId.isBlank()) {
                redirectUrl.append("&patientId=").append(patientId);
            }
            if (doctorId != null && !doctorId.isBlank()) {
                redirectUrl.append("&doctorId=").append(doctorId);
            }

            return redirectUrl.toString();
        }

        StringBuilder redirectUrl = new StringBuilder("redirect:/receptionist/dashboard/appointments?page=" + page);

        if (patientId != null && !patientId.isBlank()) {
            redirectUrl.append("&patientId=").append(patientId);
        }
        if (doctorId != null && !doctorId.isBlank()) {
            redirectUrl.append("&doctorId=").append(doctorId);
        }
        if (status != null && !status.isBlank()) {
            redirectUrl.append("&status=").append(status);
        }
        if (from != null && !from.isBlank()) {
            redirectUrl.append("&from=").append(from);
        }
        if (end != null && !end.isBlank()) {
            redirectUrl.append("&end=").append(end);
        }

        return redirectUrl.toString();
    }

    @PostMapping("/delete-account")
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
    public String redirectToKeycloakAccount() {
        return keycloakService.getAccountRedirect();
    }

    @GetMapping("/profile/keycloak-password")
    public String redirectToKeycloakPassword() {
        return keycloakService.getPasswordRedirect();
    }

    private void addUserSelectors(Model model) {
        model.addAttribute("patients", userRepository.findAllByRoleAndDeletedFalse(RoleEnum.PATIENT));
        model.addAttribute("doctors", userRepository.findAllByRoleAndDeletedFalse(RoleEnum.DOCTOR));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        model.addAttribute("minStartTime", LocalDateTime.now().plusDays(3).format(fmt));
    }

    private User requireActiveUser(OidcUser oidcUser) {
        UUID userId = OidcUserUtil.getUserId(oidcUser);
        return userRepository.findByIdAndDeletedFalse(userId)
            .orElseThrow();
    }
    private void addCreateAppointmentFormData(Model model) {
        model.addAttribute("patients", userRepository.findAllByRoleAndDeletedFalse(RoleEnum.PATIENT));
        model.addAttribute("doctors", userRepository.findAllByRoleAndDeletedFalse(RoleEnum.DOCTOR));
    }
}
