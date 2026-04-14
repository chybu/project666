package com.project666.frontend.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.time.format.DateTimeFormatter;

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

import com.project666.backend.domain.CreateAppointmentRequest;
import com.project666.backend.domain.ListAppointmentRequest;
import com.project666.backend.domain.ListLabBillRequest;
import com.project666.backend.domain.ListLabRequestRequest;
import com.project666.backend.domain.ListPrecheckRequest;
import com.project666.backend.domain.ListPrescriptionRequest;
import com.project666.backend.domain.dto.PatientLabRequestResponseDto;
import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.entity.AppointmentBill;
import com.project666.backend.domain.entity.AppointmentStatusEnum;
import com.project666.backend.domain.entity.AppointmentTypeEnum;
import com.project666.backend.domain.entity.BaseBill;
import com.project666.backend.domain.entity.BillStatusEnum;
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
import com.project666.backend.domain.entity.LabRequestStatusEnum;
import com.project666.backend.domain.entity.Precheck;
import com.project666.backend.domain.entity.PrecheckStatusEnum;
import com.project666.backend.domain.entity.Prescription;
import com.project666.backend.domain.entity.PrescriptionStatusEnum;
import com.project666.backend.domain.entity.RoleEnum;

import com.project666.backend.domain.ListPatientRecordAccessRequest;
import com.project666.backend.domain.entity.PatientRecordAccess;
import com.project666.backend.domain.entity.PatientRecordAccessStatusEnum;
import com.project666.backend.domain.entity.PatientRecordTypeEnum;
import com.project666.backend.service.PatientRecordAccessService;
import org.springframework.web.bind.annotation.PathVariable;


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
    @RequestParam(required = false) UUID doctorId,
    @RequestParam(required = false) AppointmentTypeEnum type,
    @RequestParam(required = false) AppointmentStatusEnum status,
    @RequestParam(required = false) LocalDate from,
    @RequestParam(required = false) LocalDate end,
    Model model
) {
    User user = requireActiveUser(oidcUser);
    UUID patientId = user.getId();
    String activeFilter = normalizeAppointmentFilter(filter);

    ListAppointmentRequest request = new ListAppointmentRequest();
    request.setDoctorId(doctorId);
    request.setType(type);

    LocalDate effectiveFrom = from;
    LocalDate effectiveEnd = end;
    AppointmentStatusEnum effectiveStatus = null;

    if ("past".equals(activeFilter)) {
        if (effectiveEnd == null) {
            effectiveEnd = LocalDate.now().minusDays(1);
        }
        effectiveStatus = status;
    } else {
        if (effectiveFrom == null) {
            effectiveFrom = LocalDate.now();
        }
    }

    request.setStatus(effectiveStatus);
    request.setFrom(effectiveFrom);
    request.setEnd(effectiveEnd);

    Pageable pageable = "past".equals(activeFilter)
        ? PageRequest.of(0, 20, Sort.by("startTime").descending())
        : PageRequest.of(0, 20, Sort.by("startTime").ascending());

    Page<Appointment> appointmentPage =
        appointmentService.listAppointmentForPatient(patientId, request, pageable);

    model.addAttribute("appointments", appointmentPage.getContent());
    model.addAttribute("filter", activeFilter);
    model.addAttribute("doctorId", doctorId);
    model.addAttribute("type", type);
    model.addAttribute("status", effectiveStatus);
    model.addAttribute("from", effectiveFrom);
    model.addAttribute("end", effectiveEnd);
    model.addAttribute("doctors", userRepository.findAllByRoleAndDeletedFalse(RoleEnum.DOCTOR));
    model.addAttribute("appointmentTypes", AppointmentTypeEnum.values());
    model.addAttribute("appointmentStatuses", AppointmentStatusEnum.values());

    return "patient/dashboard/reviewAppointments";
}

@GetMapping("/dashboard/finances")
public String finances(
    @AuthenticationPrincipal OidcUser oidcUser,
    @RequestParam(required = false) LocalDate appointmentFrom,
    @RequestParam(required = false) LocalDate appointmentEnd,
    @RequestParam(required = false) BigDecimal appointmentMinAmount,
    @RequestParam(required = false) BigDecimal appointmentMaxAmount,
    @RequestParam(required = false) BillStatusEnum appointmentStatus,
    @RequestParam(required = false) LocalDate labFrom,
    @RequestParam(required = false) LocalDate labEnd,
    @RequestParam(required = false) BigDecimal labMinAmount,
    @RequestParam(required = false) BigDecimal labMaxAmount,
    @RequestParam(required = false) BillStatusEnum labStatus,
    Model model
) {
    User user = requireActiveUser(oidcUser);
    UUID patientId = user.getId();

    Pageable pageable = PageRequest.of(0, 1000, Sort.by("createdAt").descending());
    List<AppointmentBill> appointmentBills = new ArrayList<>();
    List<LabBill> labBills = new ArrayList<>();
    String appointmentBillError = null;
    String labBillError = null;

    try {
        ListAppointmentBillRequest appointmentRequest = new ListAppointmentBillRequest();
        appointmentRequest.setMinAmount(appointmentMinAmount);
        appointmentRequest.setMaxAmount(appointmentMaxAmount);
        appointmentRequest.setStatus(appointmentStatus);

        Page<AppointmentBill> appointmentBillPage =
            billService.listAppointmentBillForPatient(patientId, appointmentRequest, pageable);
        appointmentBills = filterBillsByCreatedAtRange(
            appointmentBillPage.getContent(),
            appointmentFrom,
            appointmentEnd
        );
    } catch (IllegalArgumentException e) {
        appointmentBillError = e.getMessage();
    }

    try {
        ListLabBillRequest labRequest = new ListLabBillRequest();
        labRequest.setMinAmount(labMinAmount);
        labRequest.setMaxAmount(labMaxAmount);
        labRequest.setStatus(labStatus);

        Page<LabBill> labBillPage =
            billService.listLabBillForPatient(patientId, labRequest, pageable);
        labBills = filterBillsByCreatedAtRange(
            labBillPage.getContent(),
            labFrom,
            labEnd
        );
    } catch (IllegalArgumentException e) {
        labBillError = e.getMessage();
    }

    model.addAttribute("appointmentBills", appointmentBills);
    model.addAttribute("labBills", labBills);
    model.addAttribute("appointmentFrom", appointmentFrom);
    model.addAttribute("appointmentEnd", appointmentEnd);
    model.addAttribute("appointmentMinAmount", appointmentMinAmount);
    model.addAttribute("appointmentMaxAmount", appointmentMaxAmount);
    model.addAttribute("appointmentStatus", appointmentStatus);
    model.addAttribute("labFrom", labFrom);
    model.addAttribute("labEnd", labEnd);
    model.addAttribute("labMinAmount", labMinAmount);
    model.addAttribute("labMaxAmount", labMaxAmount);
    model.addAttribute("labStatus", labStatus);
    model.addAttribute("appointmentBillError", appointmentBillError);
    model.addAttribute("labBillError", labBillError);
    model.addAttribute("billStatuses", BillStatusEnum.values());

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
    @RequestParam(required = false) LocalDate startDate,
    @RequestParam(required = false) LocalDate endDate,
    @RequestParam(required = false) Integer remainingRefills,
    @RequestParam(required = false) PrescriptionStatusEnum status,
    @RequestParam(required = false) UUID doctorId,
    Model model
) {
    User user = requireActiveUser(oidcUser);
    UUID patientId = user.getId();

    Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());

    ListPrescriptionRequest request = new ListPrescriptionRequest();
    request.setStartDate(startDate);
    request.setEndDate(endDate);
    request.setRemainingRefills(remainingRefills);
    request.setStatus(status);
    request.setDoctorId(doctorId);

    Page<Prescription> prescriptions =
        prescriptionService.listPrescriptionForPatient(patientId, request, pageable);

    model.addAttribute("prescriptions", prescriptions.getContent());
    model.addAttribute("now", java.time.LocalDateTime.now());
    model.addAttribute("startDate", startDate);
    model.addAttribute("endDate", endDate);
    model.addAttribute("remainingRefills", remainingRefills);
    model.addAttribute("status", status);
    model.addAttribute("doctorId", doctorId);
    model.addAttribute("doctors", userRepository.findAllByRoleAndDeletedFalse(RoleEnum.DOCTOR));
    model.addAttribute("prescriptionStatuses", PrescriptionStatusEnum.values());

    return "patient/dashboard/pharmacy";
}

@PostMapping("/dashboard/pharmacy/consume-refill")
public String consumeRefill(
    @RequestParam UUID prescriptionId,
    @RequestParam(required = false) LocalDate startDate,
    @RequestParam(required = false) LocalDate endDate,
    @RequestParam(required = false) Integer remainingRefills,
    @RequestParam(required = false) PrescriptionStatusEnum status,
    @RequestParam(required = false) UUID doctorId,
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

    return buildPharmacyRedirect(startDate, endDate, remainingRefills, status, doctorId);
}

@Transactional
@GetMapping("/dashboard/records")
public String records(
    @AuthenticationPrincipal OidcUser oidcUser,
    @RequestParam(required = false) LocalDate precheckFrom,
    @RequestParam(required = false) LocalDate precheckEnd,
    @RequestParam(required = false) UUID precheckNurseId,
    @RequestParam(required = false) PrecheckStatusEnum precheckStatus,
    @RequestParam(required = false) LocalDate labFrom,
    @RequestParam(required = false) LocalDate labEnd,
    @RequestParam(required = false) UUID labDoctorId,
    @RequestParam(required = false) LabRequestStatusEnum labStatus,
    Model model
) {
    User user = requireActiveUser(oidcUser);
    UUID patientId = user.getId();

    Pageable precheckPageable = PageRequest.of(0, 1000, Sort.by("createdAt").descending());
    Pageable labPageable = PageRequest.of(0, 1000, Sort.by("createdAt").descending());

    ListPrecheckRequest precheckRequest = new ListPrecheckRequest();
    precheckRequest.setNurseId(precheckNurseId);
    precheckRequest.setStatus(precheckStatus);

    List<Precheck> prechecks = new ArrayList<>();
    String precheckFilterError = null;
    try {
        Page<Precheck> precheckPage =
            precheckService.listPrecheckForPatient(patientId, precheckRequest, precheckPageable);
        prechecks = filterPrechecksByCreatedAtRange(
            precheckPage.getContent(),
            precheckFrom,
            precheckEnd
        );
    } catch (IllegalArgumentException e) {
        precheckFilterError = e.getMessage();
    }

    ListLabRequestRequest labRequest = new ListLabRequestRequest();
    labRequest.setDoctorId(labDoctorId);
    labRequest.setStatus(labStatus);

    List<PatientLabRequestResponseDto> safeLabRequests = new ArrayList<>();
    String labRequestFilterError = null;
    try {
        Page<PatientLabRequestResponseDto> labRequestPage =
            labService.listLabRequestForPatient(patientId, labRequest, labPageable);

        for (PatientLabRequestResponseDto dto : labRequestPage.getContent()) {
            if (dto.getLabTests() == null) {
                dto.setLabTests(new ArrayList<>());
            }
            safeLabRequests.add(dto);
        }
        safeLabRequests = filterLabRequestsByCreatedAtRange(safeLabRequests, labFrom, labEnd);
    } catch (IllegalArgumentException e) {
        labRequestFilterError = e.getMessage();
    }

    model.addAttribute("prechecks", prechecks);
    model.addAttribute("labRequests", safeLabRequests);
    model.addAttribute("precheckFrom", precheckFrom);
    model.addAttribute("precheckEnd", precheckEnd);
    model.addAttribute("precheckNurseId", precheckNurseId);
    model.addAttribute("precheckStatus", precheckStatus);
    model.addAttribute("precheckStatuses", PrecheckStatusEnum.values());
    model.addAttribute("nurses", userRepository.findAllByRoleAndDeletedFalse(RoleEnum.NURSE));
    model.addAttribute("precheckFilterError", precheckFilterError);
    model.addAttribute("labFrom", labFrom);
    model.addAttribute("labEnd", labEnd);
    model.addAttribute("labDoctorId", labDoctorId);
    model.addAttribute("labStatus", labStatus);
    model.addAttribute("labRequestStatuses", LabRequestStatusEnum.values());
    model.addAttribute("doctors", userRepository.findAllByRoleAndDeletedFalse(RoleEnum.DOCTOR));
    model.addAttribute("labRequestFilterError", labRequestFilterError);

    return "patient/dashboard/records";
}

@Transactional
@GetMapping("/dashboard/accessRequests")
public String accessRequests(
    @AuthenticationPrincipal OidcUser oidcUser,
    @RequestParam(required = false) UUID allDoctorId,
    @RequestParam(required = false) PatientRecordTypeEnum allRecordType,
    @RequestParam(required = false) PatientRecordAccessStatusEnum allStatus,
    @RequestParam(required = false) LocalDate allFrom,
    @RequestParam(required = false) LocalDate allEnd,
    @RequestParam(required = false) UUID pendingDoctorId,
    @RequestParam(required = false) PatientRecordTypeEnum pendingRecordType,
    @RequestParam(required = false) LocalDate pendingFrom,
    @RequestParam(required = false) LocalDate pendingEnd,
    @RequestParam(required = false) UUID approvedDoctorId,
    @RequestParam(required = false) PatientRecordTypeEnum approvedRecordType,
    @RequestParam(required = false) LocalDate approvedFrom,
    @RequestParam(required = false) LocalDate approvedEnd,
    @RequestParam(required = false) UUID rejectedDoctorId,
    @RequestParam(required = false) PatientRecordTypeEnum rejectedRecordType,
    @RequestParam(required = false) LocalDate rejectedFrom,
    @RequestParam(required = false) LocalDate rejectedEnd,
    @RequestParam(required = false) UUID revokedDoctorId,
    @RequestParam(required = false) PatientRecordTypeEnum revokedRecordType,
    @RequestParam(required = false) LocalDate revokedFrom,
    @RequestParam(required = false) LocalDate revokedEnd,
    Model model
) {
    User user = requireActiveUser(oidcUser);
    UUID patientId = user.getId();

    Pageable pageable = PageRequest.of(0, 1000, Sort.by("createdAt").descending());

    List<PatientRecordAccess> allRequests = new ArrayList<>();
    List<PatientRecordAccess> pendingRequests = new ArrayList<>();
    List<PatientRecordAccess> approvedRequests = new ArrayList<>();
    List<PatientRecordAccess> rejectedRequests = new ArrayList<>();
    List<PatientRecordAccess> revokedRequests = new ArrayList<>();

    String allFilterError = null;
    String pendingFilterError = null;
    String approvedFilterError = null;
    String rejectedFilterError = null;
    String revokedFilterError = null;

    try {
        allRequests = listPatientAccessRequests(
            patientId,
            allDoctorId,
            allRecordType,
            allStatus,
            allFrom,
            allEnd,
            pageable
        );
    } catch (IllegalArgumentException e) {
        allFilterError = e.getMessage();
    }

    try {
        pendingRequests = listPatientAccessRequests(
            patientId,
            pendingDoctorId,
            pendingRecordType,
            PatientRecordAccessStatusEnum.PENDING,
            pendingFrom,
            pendingEnd,
            pageable
        );
    } catch (IllegalArgumentException e) {
        pendingFilterError = e.getMessage();
    }

    try {
        approvedRequests = listPatientAccessRequests(
            patientId,
            approvedDoctorId,
            approvedRecordType,
            PatientRecordAccessStatusEnum.APPROVED,
            approvedFrom,
            approvedEnd,
            pageable
        );
    } catch (IllegalArgumentException e) {
        approvedFilterError = e.getMessage();
    }

    try {
        rejectedRequests = listPatientAccessRequests(
            patientId,
            rejectedDoctorId,
            rejectedRecordType,
            PatientRecordAccessStatusEnum.REJECTED,
            rejectedFrom,
            rejectedEnd,
            pageable
        );
    } catch (IllegalArgumentException e) {
        rejectedFilterError = e.getMessage();
    }

    try {
        revokedRequests = listPatientAccessRequests(
            patientId,
            revokedDoctorId,
            revokedRecordType,
            PatientRecordAccessStatusEnum.REVOKED,
            revokedFrom,
            revokedEnd,
            pageable
        );
    } catch (IllegalArgumentException e) {
        revokedFilterError = e.getMessage();
    }

    model.addAttribute("allRequests", allRequests);
    model.addAttribute("pendingRequests", pendingRequests);
    model.addAttribute("approvedRequests", approvedRequests);
    model.addAttribute("rejectedRequests", rejectedRequests);
    model.addAttribute("revokedRequests", revokedRequests);
    model.addAttribute("allDoctorId", allDoctorId);
    model.addAttribute("allRecordType", allRecordType);
    model.addAttribute("allStatus", allStatus);
    model.addAttribute("allFrom", allFrom);
    model.addAttribute("allEnd", allEnd);
    model.addAttribute("pendingDoctorId", pendingDoctorId);
    model.addAttribute("pendingRecordType", pendingRecordType);
    model.addAttribute("pendingFrom", pendingFrom);
    model.addAttribute("pendingEnd", pendingEnd);
    model.addAttribute("approvedDoctorId", approvedDoctorId);
    model.addAttribute("approvedRecordType", approvedRecordType);
    model.addAttribute("approvedFrom", approvedFrom);
    model.addAttribute("approvedEnd", approvedEnd);
    model.addAttribute("rejectedDoctorId", rejectedDoctorId);
    model.addAttribute("rejectedRecordType", rejectedRecordType);
    model.addAttribute("rejectedFrom", rejectedFrom);
    model.addAttribute("rejectedEnd", rejectedEnd);
    model.addAttribute("revokedDoctorId", revokedDoctorId);
    model.addAttribute("revokedRecordType", revokedRecordType);
    model.addAttribute("revokedFrom", revokedFrom);
    model.addAttribute("revokedEnd", revokedEnd);
    model.addAttribute("allFilterError", allFilterError);
    model.addAttribute("pendingFilterError", pendingFilterError);
    model.addAttribute("approvedFilterError", approvedFilterError);
    model.addAttribute("rejectedFilterError", rejectedFilterError);
    model.addAttribute("revokedFilterError", revokedFilterError);
    model.addAttribute("doctors", userRepository.findAllByRoleAndDeletedFalse(RoleEnum.DOCTOR));
    model.addAttribute("recordTypes", PatientRecordTypeEnum.values());
    model.addAttribute("accessRequestStatuses", PatientRecordAccessStatusEnum.values());

    return "patient/dashboard/accessRequests";
}


@PostMapping("/dashboard/accessRequests/{id}/approve")
public String approveAccessRequest(
    @PathVariable UUID id,
    @AuthenticationPrincipal OidcUser oidcUser,
    @RequestParam Map<String, String> redirectParams,
    org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes
) {
    User user = requireActiveUser(oidcUser);
    try {
        patientRecordAccessService.approve(user.getId(), id);
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return buildAccessRequestsRedirect(redirectParams);
}

@PostMapping("/dashboard/accessRequests/{id}/deny")
public String denyAccessRequest(
    @PathVariable UUID id,
    @AuthenticationPrincipal OidcUser oidcUser,
    @RequestParam Map<String, String> redirectParams,
    org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes
) {
    User user = requireActiveUser(oidcUser);
    try {
        patientRecordAccessService.deny(user.getId(), id);
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return buildAccessRequestsRedirect(redirectParams);
}

@PostMapping("/dashboard/accessRequests/{id}/revoke")
public String revokeAccessRequest(
    @PathVariable UUID id,
    @AuthenticationPrincipal OidcUser oidcUser,
    @RequestParam Map<String, String> redirectParams,
    org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes
) {
    User user = requireActiveUser(oidcUser);
    try {
        patientRecordAccessService.revoke(user.getId(), id);
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return buildAccessRequestsRedirect(redirectParams);
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
        @RequestParam(required = false) String filter,
        @RequestParam(required = false) UUID doctorId,
        @RequestParam(required = false) AppointmentTypeEnum type,
        @RequestParam(required = false) LocalDate from,
        @RequestParam(required = false) LocalDate end,
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

    return buildReviewAppointmentsRedirect(filter, doctorId, type, from, end);
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

    @GetMapping("/dashboard/bookAppointment")
    public String showBookAppointmentPage(
            @AuthenticationPrincipal OidcUser oidcUser,
            Model model
    ) {
        requireActiveUser(oidcUser);

        List<User> doctors = userRepository.findAllByRoleAndDeletedFalse(RoleEnum.DOCTOR);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        model.addAttribute("minDateTime", java.time.LocalDateTime.now().plusDays(3).format(fmt));
        model.addAttribute("maxDateTime", java.time.LocalDateTime.now().plusDays(31).format(fmt));
        model.addAttribute("doctors", doctors);
        model.addAttribute("appointmentTypes", AppointmentTypeEnum.values());

        return "patient/dashboard/bookAppointment";
    }

    @PostMapping("/dashboard/book-appointment")
    public String bookAppointment(
            @AuthenticationPrincipal OidcUser oidcUser,
            @RequestParam UUID doctorId,
            @RequestParam AppointmentTypeEnum type,
            @RequestParam String startTime,
            RedirectAttributes redirectAttributes
    ) {
        User user = requireActiveUser(oidcUser);

        try {
            CreateAppointmentRequest request = new CreateAppointmentRequest();
            request.setPatientId(user.getId());
            request.setDoctorId(doctorId);
            request.setType(type);
            request.setStartTime(java.time.LocalDateTime.parse(startTime));

            appointmentService.createAppointmentForPatient(user.getId(), request);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/patient/dashboard/bookAppointment";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Appointment booked successfully.");
        return "redirect:/patient/dashboard/reviewAppointments";
    }

    private User requireActiveUser(OidcUser oidcUser) {
        UUID userId = OidcUserUtil.getUserId(oidcUser);
        return userRepository.findByIdAndDeletedFalse(userId)
            .orElseThrow();
    }

    private String normalizeAppointmentFilter(String filter) {
        return "past".equalsIgnoreCase(filter) ? "past" : "upcoming";
    }

    private String buildReviewAppointmentsRedirect(
        String filter,
        UUID doctorId,
        AppointmentTypeEnum type,
        LocalDate from,
        LocalDate end
    ) {
        StringBuilder redirectUrl = new StringBuilder(
            "redirect:/patient/dashboard/reviewAppointments?filter=" + normalizeAppointmentFilter(filter)
        );

        if (doctorId != null) {
            redirectUrl.append("&doctorId=").append(doctorId);
        }

        if (type != null) {
            redirectUrl.append("&type=").append(type);
        }

        if (from != null) {
            redirectUrl.append("&from=").append(from);
        }

        if (end != null) {
            redirectUrl.append("&end=").append(end);
        }

        return redirectUrl.toString();
    }

    private String buildPharmacyRedirect(
        LocalDate startDate,
        LocalDate endDate,
        Integer remainingRefills,
        PrescriptionStatusEnum status,
        UUID doctorId
    ) {
        StringBuilder redirectUrl = new StringBuilder("redirect:/patient/dashboard/pharmacy");
        List<String> queryParams = new ArrayList<>();

        if (startDate != null) {
            queryParams.add("startDate=" + startDate);
        }

        if (endDate != null) {
            queryParams.add("endDate=" + endDate);
        }

        if (remainingRefills != null) {
            queryParams.add("remainingRefills=" + remainingRefills);
        }

        if (status != null) {
            queryParams.add("status=" + status);
        }

        if (doctorId != null) {
            queryParams.add("doctorId=" + doctorId);
        }

        if (!queryParams.isEmpty()) {
            redirectUrl.append("?").append(String.join("&", queryParams));
        }

        return redirectUrl.toString();
    }

    private <T extends BaseBill> List<T> filterBillsByCreatedAtRange(
        List<T> bills,
        LocalDate from,
        LocalDate end
    ) {
        if (from != null && end != null && from.isAfter(end)) {
            throw new IllegalArgumentException("from must be on or before end");
        }

        if (from == null && end == null) {
            return bills;
        }

        return bills.stream()
            .filter(bill -> {
                LocalDate createdAtDate = bill.getCreatedAt().toLocalDate();
                return (from == null || !createdAtDate.isBefore(from))
                    && (end == null || !createdAtDate.isAfter(end));
            })
            .toList();
    }

    private List<Precheck> filterPrechecksByCreatedAtRange(
        List<Precheck> prechecks,
        LocalDate from,
        LocalDate end
    ) {
        if (from != null && end != null && from.isAfter(end)) {
            throw new IllegalArgumentException("precheckFrom must be on or before precheckEnd");
        }

        if (from == null && end == null) {
            return prechecks;
        }

        return prechecks.stream()
            .filter(precheck -> {
                LocalDate createdAtDate = precheck.getCreatedAt().toLocalDate();
                return (from == null || !createdAtDate.isBefore(from))
                    && (end == null || !createdAtDate.isAfter(end));
            })
            .toList();
    }

    private List<PatientLabRequestResponseDto> filterLabRequestsByCreatedAtRange(
        List<PatientLabRequestResponseDto> labRequests,
        LocalDate from,
        LocalDate end
    ) {
        if (from != null && end != null && from.isAfter(end)) {
            throw new IllegalArgumentException("labFrom must be on or before labEnd");
        }

        if (from == null && end == null) {
            return labRequests;
        }

        return labRequests.stream()
            .filter(labRequest -> {
                LocalDate createdAtDate = labRequest.getCreatedAt().toLocalDate();
                return (from == null || !createdAtDate.isBefore(from))
                    && (end == null || !createdAtDate.isAfter(end));
            })
            .toList();
    }

    private List<PatientRecordAccess> listPatientAccessRequests(
        UUID patientId,
        UUID doctorId,
        PatientRecordTypeEnum recordType,
        PatientRecordAccessStatusEnum status,
        LocalDate from,
        LocalDate end,
        Pageable pageable
    ) {
        ListPatientRecordAccessRequest request = new ListPatientRecordAccessRequest();
        request.setDoctorId(doctorId);
        request.setType(recordType);
        request.setStatus(status);

        Page<PatientRecordAccess> accessPage =
            patientRecordAccessService.listPatientRecordAccess(patientId, request, pageable);

        return filterAccessRequestsByCreatedAtRange(accessPage.getContent(), from, end);
    }

    private List<PatientRecordAccess> filterAccessRequestsByCreatedAtRange(
        List<PatientRecordAccess> accessRequests,
        LocalDate from,
        LocalDate end
    ) {
        if (from != null && end != null && from.isAfter(end)) {
            throw new IllegalArgumentException("min date must be on or before max date");
        }

        if (from == null && end == null) {
            return accessRequests;
        }

        return accessRequests.stream()
            .filter(accessRequest -> {
                LocalDate createdAtDate = accessRequest.getCreatedAt().toLocalDate();
                return (from == null || !createdAtDate.isBefore(from))
                    && (end == null || !createdAtDate.isAfter(end));
            })
            .toList();
    }

    private String buildAccessRequestsRedirect(Map<String, String> redirectParams) {
        StringBuilder redirectUrl = new StringBuilder("redirect:/patient/dashboard/accessRequests");
        List<String> queryParams = new ArrayList<>();

        appendQueryParam(queryParams, "allDoctorId", redirectParams.get("allDoctorId"));
        appendQueryParam(queryParams, "allRecordType", redirectParams.get("allRecordType"));
        appendQueryParam(queryParams, "allStatus", redirectParams.get("allStatus"));
        appendQueryParam(queryParams, "allFrom", redirectParams.get("allFrom"));
        appendQueryParam(queryParams, "allEnd", redirectParams.get("allEnd"));
        appendQueryParam(queryParams, "pendingDoctorId", redirectParams.get("pendingDoctorId"));
        appendQueryParam(queryParams, "pendingRecordType", redirectParams.get("pendingRecordType"));
        appendQueryParam(queryParams, "pendingFrom", redirectParams.get("pendingFrom"));
        appendQueryParam(queryParams, "pendingEnd", redirectParams.get("pendingEnd"));
        appendQueryParam(queryParams, "approvedDoctorId", redirectParams.get("approvedDoctorId"));
        appendQueryParam(queryParams, "approvedRecordType", redirectParams.get("approvedRecordType"));
        appendQueryParam(queryParams, "approvedFrom", redirectParams.get("approvedFrom"));
        appendQueryParam(queryParams, "approvedEnd", redirectParams.get("approvedEnd"));
        appendQueryParam(queryParams, "rejectedDoctorId", redirectParams.get("rejectedDoctorId"));
        appendQueryParam(queryParams, "rejectedRecordType", redirectParams.get("rejectedRecordType"));
        appendQueryParam(queryParams, "rejectedFrom", redirectParams.get("rejectedFrom"));
        appendQueryParam(queryParams, "rejectedEnd", redirectParams.get("rejectedEnd"));
        appendQueryParam(queryParams, "revokedDoctorId", redirectParams.get("revokedDoctorId"));
        appendQueryParam(queryParams, "revokedRecordType", redirectParams.get("revokedRecordType"));
        appendQueryParam(queryParams, "revokedFrom", redirectParams.get("revokedFrom"));
        appendQueryParam(queryParams, "revokedEnd", redirectParams.get("revokedEnd"));

        if (!queryParams.isEmpty()) {
            redirectUrl.append("?").append(String.join("&", queryParams));
        }

        return redirectUrl.toString();
    }

    private void appendQueryParam(List<String> queryParams, String key, String value) {
        if (value != null && !value.isBlank()) {
            queryParams.add(key + "=" + value);
        }
    }

}
