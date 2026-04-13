package com.project666.frontend.controller;

import java.time.LocalDate;
import java.util.UUID;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.transaction.annotation.Transactional;

import com.project666.backend.domain.ListAppointmentRequest;
import com.project666.backend.domain.ListLabBillRequest;
import com.project666.backend.domain.ListLabRequestRequest;
import com.project666.backend.domain.ListPrecheckRequest;
import com.project666.backend.domain.ListPrescriptionRequest;
import com.project666.backend.domain.dto.PatientLabRequestResponseDto;
import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.entity.AppointmentBill;
import com.project666.backend.domain.entity.AppointmentStatusEnum;
import com.project666.backend.service.AppointmentService;
import com.project666.backend.service.BillService;
import com.project666.backend.service.LabService;
import com.project666.backend.service.PrecheckService;
import com.project666.backend.service.PrescriptionService;
import com.project666.frontend.service.KeycloakService;
import com.project666.frontend.util.OidcUserUtil;
import com.project666.backend.domain.entity.User;
import com.project666.backend.repository.UserRepository;
import com.project666.backend.domain.CancelAppointmentRequest;
import com.project666.backend.domain.ListAppointmentBillRequest;
import com.project666.backend.domain.entity.CancellationInitiatorEnum;
import com.project666.backend.domain.entity.LabBill;
import com.project666.backend.domain.entity.Precheck;
import com.project666.backend.domain.entity.Prescription;
import com.project666.backend.domain.entity.RoleEnum;

import com.project666.backend.domain.ListPatientRecordAccessRequest;
import com.project666.backend.domain.entity.PatientRecordAccess;
import com.project666.backend.domain.entity.PatientRecordAccessStatusEnum;
import com.project666.backend.service.PatientRecordAccessService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import lombok.RequiredArgsConstructor;



@Controller
@RequiredArgsConstructor
@RequestMapping("/patient")
@PreAuthorize("hasRole('PATIENT')")
public class PatientController {

    private final AppointmentService appointmentService;
    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final BillService billService;
    private final PrescriptionService prescriptionService;
    private final LabService labService;
    private final PrecheckService precheckService;
    private final PatientRecordAccessService patientRecordAccessService;


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
    @RequestParam(required = false) String filter,
    Model model
) {
    User user = requireActiveUser(oidcUser);
    UUID patientId = user.getId();

    ListAppointmentRequest request = new ListAppointmentRequest();

    if ("past".equalsIgnoreCase(filter)) {
        request.setFrom(LocalDate.of(1900, 1, 1));
    } else {
        request.setFrom(LocalDate.now());
    }

    Pageable pageable = PageRequest.of(0, 20, Sort.by("startTime").ascending());

    Page<Appointment> appointmentPage =
        appointmentService.listAppointmentForPatient(patientId, request, pageable);

    model.addAttribute("appointments", appointmentPage.getContent());
    model.addAttribute("filter", filter == null ? "upcoming" : filter);

    return "patient/dashboard/reviewAppointments";
}

@GetMapping("/dashboard/finances")
public String finances(
    @AuthenticationPrincipal OidcUser oidcUser,
    Model model
) {
    User user = requireActiveUser(oidcUser);
    UUID patientId = user.getId();

    Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());


    ListAppointmentBillRequest appointmentRequest = new ListAppointmentBillRequest();

    Page<AppointmentBill> appointmentBills =
        billService.listAppointmentBillForPatient(patientId, appointmentRequest, pageable);

    ListLabBillRequest labRequest = new ListLabBillRequest();

    Page<LabBill> labBills =
        billService.listLabBillForPatient(patientId, labRequest, pageable);

    model.addAttribute("appointmentBills", appointmentBills.getContent());
    model.addAttribute("labBills", labBills.getContent());

    return "patient/dashboard/finances";
}

    @GetMapping("/dashboard/notifications")
    public String notifications() {
        return "patient/dashboard/notifications";
    }

@Transactional
@GetMapping("/dashboard/pharmacy")
public String pharmacy(
    @AuthenticationPrincipal OidcUser oidcUser,
    Model model
) {
    User user = requireActiveUser(oidcUser);
    UUID patientId = user.getId();

    Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());

    ListPrescriptionRequest request = new ListPrescriptionRequest();

    Page<Prescription> prescriptions =
        prescriptionService.listPrescriptionForPatient(patientId, request, pageable);

    model.addAttribute("prescriptions", prescriptions.getContent());

    return "patient/dashboard/pharmacy";
}

@PostMapping("/dashboard/pharmacy/consume-refill")
public String consumeRefill(
    @RequestParam UUID prescriptionId,
    @AuthenticationPrincipal OidcUser oidcUser,
    org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes
) {
    User user = requireActiveUser(oidcUser);
    UUID patientId = user.getId();

    try {
        prescriptionService.consumeRefill(patientId, prescriptionId);
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }

    return "redirect:/patient/dashboard/pharmacy";
}

@Transactional
@GetMapping("/dashboard/records")
public String records(
    @AuthenticationPrincipal OidcUser oidcUser,
    Model model
) {
    User user = requireActiveUser(oidcUser);
    UUID patientId = user.getId();

    Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());

    ListPrecheckRequest precheckRequest = new ListPrecheckRequest();
    Page<Precheck> prechecks =
        precheckService.listPrecheckForPatient(patientId, precheckRequest, pageable);

    ListLabRequestRequest labRequest = new ListLabRequestRequest();
    Page<PatientLabRequestResponseDto> labRequestPage =
        labService.listLabRequestForPatient(patientId, labRequest, pageable);

    List<PatientLabRequestResponseDto> safeLabRequests = new ArrayList<>();
    for (PatientLabRequestResponseDto dto : labRequestPage.getContent()) {
        if (dto.getLabTests() == null) {
            dto.setLabTests(new ArrayList<>());
        }
        safeLabRequests.add(dto);
    }

    model.addAttribute("prechecks", prechecks.getContent());
    model.addAttribute("labRequests", safeLabRequests);

    return "patient/dashboard/records";
}

@Transactional
@GetMapping("/dashboard/accessRequests")
public String accessRequests(
    @AuthenticationPrincipal OidcUser oidcUser,
    Model model
) {
    User user = requireActiveUser(oidcUser);
    UUID patientId = user.getId();

    Pageable pageable = PageRequest.of(0, 50, Sort.by("createdAt").descending());

    ListPatientRecordAccessRequest allRequest = new ListPatientRecordAccessRequest();
    ListPatientRecordAccessRequest pendingRequest = new ListPatientRecordAccessRequest();
    ListPatientRecordAccessRequest approvedRequest = new ListPatientRecordAccessRequest();
    ListPatientRecordAccessRequest rejectedRequest = new ListPatientRecordAccessRequest();
    ListPatientRecordAccessRequest revokedRequest = new ListPatientRecordAccessRequest();

    pendingRequest.setStatus(PatientRecordAccessStatusEnum.PENDING);
    approvedRequest.setStatus(PatientRecordAccessStatusEnum.APPROVED);
    rejectedRequest.setStatus(PatientRecordAccessStatusEnum.REJECTED);
    revokedRequest.setStatus(PatientRecordAccessStatusEnum.REVOKED);

    model.addAttribute("allRequests", patientRecordAccessService.listPatientRecordAccess(patientId, allRequest, pageable).getContent());
    model.addAttribute("pendingRequests", patientRecordAccessService.listPatientRecordAccess(patientId, pendingRequest, pageable).getContent());
    model.addAttribute("approvedRequests", patientRecordAccessService.listPatientRecordAccess(patientId, approvedRequest, pageable).getContent());
    model.addAttribute("rejectedRequests", patientRecordAccessService.listPatientRecordAccess(patientId, rejectedRequest, pageable).getContent());
    model.addAttribute("revokedRequests", patientRecordAccessService.listPatientRecordAccess(patientId, revokedRequest, pageable).getContent());

    return "patient/dashboard/accessRequests";
}


@PostMapping("/dashboard/accessRequests/{id}/approve")
public String approveAccessRequest(
    @PathVariable UUID id,
    @AuthenticationPrincipal OidcUser oidcUser,
    org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes
) {
    User user = requireActiveUser(oidcUser);
    try {
        patientRecordAccessService.approve(user.getId(), id);
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/patient/dashboard/accessRequests";
}

@PostMapping("/dashboard/accessRequests/{id}/deny")
public String denyAccessRequest(
    @PathVariable UUID id,
    @AuthenticationPrincipal OidcUser oidcUser,
    org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes
) {
    User user = requireActiveUser(oidcUser);
    try {
        patientRecordAccessService.deny(user.getId(), id);
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/patient/dashboard/accessRequests";
}

@PostMapping("/dashboard/accessRequests/{id}/revoke")
public String revokeAccessRequest(
    @PathVariable UUID id,
    @AuthenticationPrincipal OidcUser oidcUser,
    org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes
) {
    User user = requireActiveUser(oidcUser);
    try {
        patientRecordAccessService.revoke(user.getId(), id);
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/patient/dashboard/accessRequests";
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
        @AuthenticationPrincipal OidcUser oidcUser,
        RedirectAttributes redirectAttributes
) {
    UUID userId = OidcUserUtil.getUserId(oidcUser);

    try {
        CancelAppointmentRequest request = new CancelAppointmentRequest();
        request.setAppointmentId(appointmentId);
        request.setCancelReason("Cancelled by patient from dashboard");
        request.setCancellationInitiator(CancellationInitiatorEnum.PATIENT);

        appointmentService.cancelAppointment(
                userId,
                RoleEnum.PATIENT,
                request
        );

    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
    }

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
