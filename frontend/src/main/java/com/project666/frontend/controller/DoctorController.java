package com.project666.frontend.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.project666.frontend.service.KeycloakService;
import com.project666.frontend.util.OidcUserUtil;
import com.project666.backend.domain.entity.User;
import com.project666.backend.repository.UserRepository;
import com.project666.backend.domain.CancelAppointmentRequest;
import com.project666.backend.domain.CreateLabRequestRequest;
import com.project666.backend.domain.CreatePrescriptionRequest;
import com.project666.backend.domain.entity.CancellationInitiatorEnum;
import com.project666.backend.domain.entity.LabRequest;
import com.project666.backend.domain.entity.PatientRecordAccess;
import com.project666.backend.domain.entity.PatientRecordTypeEnum;
import com.project666.backend.domain.entity.Precheck;
import com.project666.backend.domain.entity.PrecheckStatusEnum;
import com.project666.backend.domain.entity.Prescription;
import com.project666.backend.domain.entity.RoleEnum;
import com.project666.backend.domain.entity.AppointmentStatusEnum;


import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.project666.backend.domain.ListAppointmentRequest;
import com.project666.backend.domain.ListLabRequestRequest;
import com.project666.backend.domain.ListPatientRecordAccessRequest;
import com.project666.backend.domain.ListPrecheckRequest;
import com.project666.backend.domain.ListPrescriptionRequest;
import com.project666.backend.domain.PatientRecordAccessRequest;
import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.service.AppointmentService;
import com.project666.backend.service.LabService;
import com.project666.backend.service.PatientRecordAccessService;
import com.project666.backend.service.PrecheckService;
import com.project666.backend.service.PrescriptionService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestParam;




@Controller
@RequiredArgsConstructor
@RequestMapping("/doctor")
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorController {

    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final AppointmentService appointmentService;
    private final PrecheckService precheckService;
    private final PrescriptionService prescriptionService;
    private final LabService labService;
    private final PatientRecordAccessService patientRecordAccessService;


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
        UUID doctorId = user.getId();

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
                appointmentService.listAppointmentForDoctor(doctorId, request, pageable);

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

        LocalDate effectiveSelectedDate = selectedDate;

        List<Appointment> selectedDayAppointments =
                (effectiveSelectedDate != null && appointmentsByDate.containsKey(effectiveSelectedDate))
                        ? appointmentsByDate.get(effectiveSelectedDate)
                        : new ArrayList<>();

        model.addAttribute("selectedDate", effectiveSelectedDate);
        model.addAttribute("selectedDayAppointments", selectedDayAppointments);

        YearMonth prevMonth = yearMonth.minusMonths(1);
        YearMonth nextMonth = yearMonth.plusMonths(1);

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

        return "doctor/dashboard/home";
    }

@GetMapping("/dashboard/appointments")
public String appointments(
        @AuthenticationPrincipal OidcUser oidcUser,
        Model model
) {
    UUID doctorId = OidcUserUtil.getUserId(oidcUser);

    Pageable pageable = PageRequest.of(0, 50, Sort.by("startTime").ascending());

    ListAppointmentRequest upcomingRequest = new ListAppointmentRequest();
    upcomingRequest.setStatus(AppointmentStatusEnum.CONFIRMED);
    upcomingRequest.setFrom(LocalDate.now());

    Page<Appointment> upcomingPage =
            appointmentService.listAppointmentForDoctor(doctorId, upcomingRequest, pageable);

    ListAppointmentRequest completedRequest = new ListAppointmentRequest();
    completedRequest.setStatus(AppointmentStatusEnum.COMPLETED);

    Page<Appointment> completedPage =
            appointmentService.listAppointmentForDoctor(doctorId, completedRequest, pageable);

    model.addAttribute("upcomingAppointments", upcomingPage.getContent());
    model.addAttribute("completedAppointments", completedPage.getContent());

    return "doctor/dashboard/appointments";
}

@GetMapping("/dashboard/prechecks")
public String prechecks(
        @AuthenticationPrincipal OidcUser oidcUser,
        Model model
) {
    UUID doctorId = OidcUserUtil.getUserId(oidcUser);

    ListPrecheckRequest request = new ListPrecheckRequest();
    request.setStatus(PrecheckStatusEnum.VALID);

    Pageable pageable = PageRequest.of(0, 50, Sort.by("createdAt").descending());

    Page<Precheck> page =
            precheckService.listPrecheckForDoctor(doctorId, request, pageable);

    model.addAttribute("prechecks", page.getContent());

    return "doctor/dashboard/prechecks";
}

@GetMapping("/dashboard/prescriptions")
public String prescriptions(
        @AuthenticationPrincipal OidcUser oidcUser,
        Model model
) {
    UUID doctorId = OidcUserUtil.getUserId(oidcUser);

    ListPrescriptionRequest request = new ListPrescriptionRequest();

    Pageable pageable = PageRequest.of(0, 50, Sort.by("createdAt").descending());

    Page<Prescription> page =
            prescriptionService.listPrescriptionForDoctor(doctorId, request, pageable);

    model.addAttribute("prescriptions", page.getContent());

    return "doctor/dashboard/prescriptions";
}

@PostMapping("/prescriptions/create")
public String createPrescription(
        @AuthenticationPrincipal OidcUser oidcUser,
        @ModelAttribute CreatePrescriptionRequest request,
        Model model
) {
    UUID doctorId = OidcUserUtil.getUserId(oidcUser);

    try {
        prescriptionService.createPrescription(doctorId, request);
    } catch (Exception e) {
        model.addAttribute("error", e.getMessage());
        return "doctor/dashboard/prescriptions";
    }

    return "redirect:/doctor/dashboard/prescriptions";
}

@PostMapping("/prescriptions/cancel")
public String cancelPrescription(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RequestParam UUID prescriptionId
) {
    UUID doctorId = OidcUserUtil.getUserId(oidcUser);

    prescriptionService.cancelPrescription(doctorId, prescriptionId);

    return "redirect:/doctor/dashboard/prescriptions";
}

@GetMapping("/prescriptions/create-form")
public String showCreatePrescriptionForm(
        @RequestParam UUID appointmentId,
        Model model
) {
    CreatePrescriptionRequest request = new CreatePrescriptionRequest();
    request.setAppointmentId(appointmentId);

    model.addAttribute("request", request);

    return "doctor/dashboard/create-prescription";
}

@GetMapping("/dashboard/labs")
public String labs(
        @AuthenticationPrincipal OidcUser oidcUser,
        Model model
) {
    UUID doctorId = OidcUserUtil.getUserId(oidcUser);

    ListLabRequestRequest request = new ListLabRequestRequest();

    Pageable pageable = PageRequest.of(0, 50, Sort.by("createdAt").descending());

    Page<LabRequest> page =
            labService.listLabRequestForDoctor(doctorId, request, pageable);

    model.addAttribute("labs", page.getContent());

    return "doctor/dashboard/labs";
}

@GetMapping("/labs/create-form")
public String showCreateLabForm(
        @RequestParam UUID appointmentId,
        Model model
) {
    CreateLabRequestRequest request = new CreateLabRequestRequest();
    request.setAppointmentId(appointmentId);

    model.addAttribute("request", request);

    return "doctor/dashboard/create-lab";
}

@PostMapping("/labs/create")
public String createLab(
        @AuthenticationPrincipal OidcUser oidcUser,
        @ModelAttribute CreateLabRequestRequest request,
        Model model
) {
    UUID doctorId = OidcUserUtil.getUserId(oidcUser);

    try {
        labService.createLabRequest(doctorId, request);
    } catch (Exception e) {
        model.addAttribute("error", e.getMessage());
        return "doctor/dashboard/labs";
    }

    return "redirect:/doctor/dashboard/labs";
}

@PostMapping("/labs/cancel")
public String cancelLab(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RequestParam UUID labId
) {
    UUID doctorId = OidcUserUtil.getUserId(oidcUser);

    labService.cancelLabRequest(doctorId, labId);

    return "redirect:/doctor/dashboard/labs";
}


@GetMapping("/dashboard/shared-records")
public String sharedRecords(
        @AuthenticationPrincipal OidcUser oidcUser,
        Model model
) {
    UUID doctorId = OidcUserUtil.getUserId(oidcUser);

    Pageable pageable = PageRequest.of(0, 50);

    ListPatientRecordAccessRequest accessRequest = new ListPatientRecordAccessRequest();
    Page<PatientRecordAccess> accessPage =
            patientRecordAccessService.listSharedPatientRecordAccess(doctorId, accessRequest, pageable);

    model.addAttribute("sharedAccess", accessPage.getContent());

    ListPrecheckRequest precheckRequest = new ListPrecheckRequest();
    Page<Precheck> precheckPage =
            precheckService.listSharedPrecheckForDoctor(doctorId, precheckRequest, pageable);

    model.addAttribute("prechecks", precheckPage.getContent());

    ListLabRequestRequest labRequest = new ListLabRequestRequest();
    Page<LabRequest> labPage =
            labService.listLabRequestForNewDoctor(doctorId, labRequest, pageable);

    model.addAttribute("labs", labPage.getContent());

    ListPrescriptionRequest prescriptionRequest = new ListPrescriptionRequest();
    Page<Prescription> prescriptionPage =
            prescriptionService.listPrescriptionForNewDoctor(doctorId, prescriptionRequest, pageable);

    model.addAttribute("prescriptions", prescriptionPage.getContent());

    return "doctor/dashboard/shared-records";
}


@GetMapping("/dashboard/access-requests")
public String accessRequests(
        @AuthenticationPrincipal OidcUser oidcUser,
        Model model
) {
    UUID doctorId = OidcUserUtil.getUserId(oidcUser);

    ListPatientRecordAccessRequest request = new ListPatientRecordAccessRequest();
    request.setDoctorId(doctorId);

    Pageable pageable = PageRequest.of(0, 50, Sort.by("createdAt").descending());

    Page<PatientRecordAccess> page =
            patientRecordAccessService.listPatientRecordAccess(null, request, pageable);

    model.addAttribute("accessRequests", page.getContent());

    model.addAttribute("patients",
            userRepository.findAll().stream()
                    .filter(u -> u.getRole() == RoleEnum.PATIENT)
                    .toList()
    );

    return "doctor/dashboard/access-requests";
}

@PostMapping("/access-requests/create")
public String createAccessRequest(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RequestParam UUID patientId,
        @RequestParam PatientRecordTypeEnum type,
        Model model
) {
    UUID doctorId = OidcUserUtil.getUserId(oidcUser);

    try {
        PatientRecordAccessRequest request = new PatientRecordAccessRequest();
        request.setPatientId(patientId);
        request.setType(type);

        patientRecordAccessService.requestPatientRecordAccess(doctorId, request);

    } catch (Exception e) {

        model.addAttribute("error", e.getMessage());

        ListPatientRecordAccessRequest listRequest = new ListPatientRecordAccessRequest();
        listRequest.setDoctorId(doctorId);

        Pageable pageable = PageRequest.of(0, 50, Sort.by("createdAt").descending());

        Page<PatientRecordAccess> page =
                patientRecordAccessService.listPatientRecordAccess(null, listRequest, pageable);

        model.addAttribute("accessRequests", page.getContent());

        model.addAttribute("patients",
                userRepository.findAll().stream()
                        .filter(u -> u.getRole() == RoleEnum.PATIENT)
                        .toList()
        );

        return "doctor/dashboard/access-requests";
    }

    return "redirect:/doctor/dashboard/access-requests";
}

@PostMapping("/access-requests/cancel")
public String cancelAccessRequest(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RequestParam UUID accessId
) {
    UUID doctorId = OidcUserUtil.getUserId(oidcUser);

    patientRecordAccessService.cancel(doctorId, accessId);

    return "redirect:/doctor/dashboard/access-requests";
}



    @GetMapping("/dashboard/notifications")
    public String notifications() {
        return "doctor/dashboard/notifications";
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

        return "doctor/dashboard/profile";
    }
    @PostMapping("/dashboard/cancel-appointment")
    public String cancelAppointment(
            @RequestParam UUID appointmentId,
            @AuthenticationPrincipal OidcUser oidcUser
    ) {
        UUID userId = OidcUserUtil.getUserId(oidcUser);

        CancelAppointmentRequest request = new CancelAppointmentRequest();
        request.setAppointmentId(appointmentId);
        request.setCancelReason("Cancelled by doctor from dashboard");
        request.setCancellationInitiator(CancellationInitiatorEnum.RECEPTIONIST);

        appointmentService.cancelAppointment(
                userId,
                RoleEnum.RECEPTIONIST,
                request
        );

        return "redirect:/doctor/dashboard/home";
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

    private User requireActiveUser(OidcUser oidcUser) {
        UUID userId = OidcUserUtil.getUserId(oidcUser);
        return userRepository.findByIdAndDeletedFalse(userId)
            .orElseThrow();
    }
}
