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
import com.project666.backend.domain.CreatePrescriptionMedicineRequest;
import com.project666.backend.domain.CreatePrescriptionRequest;
import com.project666.backend.domain.entity.CancellationInitiatorEnum;
import com.project666.backend.domain.entity.LabRequest;
import com.project666.backend.domain.entity.PatientRecordAccess;
import com.project666.backend.domain.entity.PatientRecordTypeEnum;
import com.project666.backend.domain.entity.Precheck;
import com.project666.backend.domain.entity.PrecheckStatusEnum;
import com.project666.backend.domain.entity.Prescription;
import com.project666.backend.domain.entity.PrescriptionStatusEnum;
import com.project666.backend.domain.entity.RoleEnum;
import com.project666.backend.domain.entity.AppointmentStatusEnum;
import com.project666.backend.domain.entity.AppointmentTypeEnum;


import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
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
        @RequestParam(required = false) LocalDate upcomingFrom,
        @RequestParam(required = false) LocalDate upcomingEnd,
        @RequestParam(required = false) AppointmentTypeEnum upcomingType,
        @RequestParam(required = false) UUID upcomingPatientId,
        @RequestParam(required = false) LocalDate completedFrom,
        @RequestParam(required = false) LocalDate completedEnd,
        @RequestParam(required = false) AppointmentTypeEnum completedType,
        @RequestParam(required = false) UUID completedPatientId,
        Model model
) {
    User user = requireActiveUser(oidcUser);
    UUID doctorId = user.getId();

    Pageable upcomingPageable = PageRequest.of(0, 50, Sort.by("startTime").ascending());
    Pageable completedPageable = PageRequest.of(0, 50, Sort.by("startTime").descending());

    List<Appointment> upcomingAppointments = new ArrayList<>();
    List<Appointment> completedAppointments = new ArrayList<>();
    String upcomingFilterError = null;
    String completedFilterError = null;

    try {
        LocalDate effectiveUpcomingFrom = upcomingFrom != null ? upcomingFrom : LocalDate.now();

        ListAppointmentRequest upcomingRequest = new ListAppointmentRequest();
        upcomingRequest.setPatientId(upcomingPatientId);
        upcomingRequest.setType(upcomingType);
        upcomingRequest.setStatus(AppointmentStatusEnum.CONFIRMED);
        upcomingRequest.setFrom(effectiveUpcomingFrom);
        upcomingRequest.setEnd(upcomingEnd);

        upcomingAppointments = filterAppointmentsByDateRange(
                appointmentService.listAppointmentForDoctor(doctorId, upcomingRequest, upcomingPageable).getContent(),
                effectiveUpcomingFrom,
                upcomingEnd,
                "upcoming"
        );
        upcomingFrom = effectiveUpcomingFrom;
    } catch (IllegalArgumentException e) {
        upcomingFilterError = e.getMessage();
    }

    try {
        ListAppointmentRequest completedRequest = new ListAppointmentRequest();
        completedRequest.setPatientId(completedPatientId);
        completedRequest.setType(completedType);
        completedRequest.setStatus(AppointmentStatusEnum.COMPLETED);
        completedRequest.setFrom(completedFrom);
        completedRequest.setEnd(completedEnd);

        completedAppointments = filterAppointmentsByDateRange(
                appointmentService.listAppointmentForDoctor(doctorId, completedRequest, completedPageable).getContent(),
                completedFrom,
                completedEnd,
                "completed"
        );
    } catch (IllegalArgumentException e) {
        completedFilterError = e.getMessage();
    }

    model.addAttribute("upcomingAppointments", upcomingAppointments);
    model.addAttribute("completedAppointments", completedAppointments);
    model.addAttribute("upcomingFrom", upcomingFrom);
    model.addAttribute("upcomingEnd", upcomingEnd);
    model.addAttribute("upcomingType", upcomingType);
    model.addAttribute("upcomingPatientId", upcomingPatientId);
    model.addAttribute("completedFrom", completedFrom);
    model.addAttribute("completedEnd", completedEnd);
    model.addAttribute("completedType", completedType);
    model.addAttribute("completedPatientId", completedPatientId);
    model.addAttribute("upcomingFilterError", upcomingFilterError);
    model.addAttribute("completedFilterError", completedFilterError);
    model.addAttribute("appointmentTypes", AppointmentTypeEnum.values());
    model.addAttribute("patients", userRepository.findAllByRoleAndDeletedFalse(RoleEnum.PATIENT));

    return "doctor/dashboard/appointments";
}

@Transactional
@GetMapping("/dashboard/prechecks")
public String prechecks(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RequestParam(required = false) LocalDate from,
        @RequestParam(required = false) LocalDate end,
        @RequestParam(required = false) AppointmentTypeEnum type,
        @RequestParam(required = false) UUID patientId,
        @RequestParam(required = false) PrecheckStatusEnum status,
        Model model
) {
    User user = requireActiveUser(oidcUser);
    UUID doctorId = user.getId();

    PrecheckStatusEnum effectiveStatus = status != null ? status : PrecheckStatusEnum.VALID;

    ListPrecheckRequest request = new ListPrecheckRequest();
    request.setPatientId(patientId);
    request.setStatus(effectiveStatus);

    Pageable pageable = PageRequest.of(0, 1000, Sort.by("createdAt").descending());

    List<Precheck> prechecks = new ArrayList<>();
    String filterError = null;

    try {
        Page<Precheck> page =
                precheckService.listPrecheckForDoctor(doctorId, request, pageable);
        prechecks = filterPrechecksForDoctor(page.getContent(), from, end, type);
    } catch (IllegalArgumentException e) {
        filterError = e.getMessage();
    }

    model.addAttribute("prechecks", prechecks);
    model.addAttribute("from", from);
    model.addAttribute("end", end);
    model.addAttribute("type", type);
    model.addAttribute("patientId", patientId);
    model.addAttribute("status", effectiveStatus);
    model.addAttribute("filterError", filterError);
    model.addAttribute("appointmentTypes", AppointmentTypeEnum.values());
    model.addAttribute("patients", userRepository.findAllByRoleAndDeletedFalse(RoleEnum.PATIENT));
    model.addAttribute("precheckStatuses", PrecheckStatusEnum.values());

    return "doctor/dashboard/prechecks";
}

@GetMapping("/dashboard/prescriptions")
public String prescriptions(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RequestParam(required = false) UUID patientId,
        @RequestParam(required = false) PrescriptionStatusEnum status,
        @RequestParam(required = false) LocalDate startDate,
        @RequestParam(required = false) LocalDate endDate,
        @RequestParam(required = false) Integer remainingRefills,
        Model model
) {
    UUID doctorId = OidcUserUtil.getUserId(oidcUser);

    ListPrescriptionRequest request = new ListPrescriptionRequest();
    request.setPatientId(patientId);
    request.setStatus(status);
    request.setStartDate(startDate);
    request.setEndDate(endDate);
    request.setRemainingRefills(remainingRefills);

    Pageable pageable = PageRequest.of(0, 50, Sort.by("createdAt").descending());

    Page<Prescription> page =
            prescriptionService.listPrescriptionForDoctor(doctorId, request, pageable);

    model.addAttribute("prescriptions", page.getContent());
    model.addAttribute("patients", userRepository.findAllByRoleAndDeletedFalse(RoleEnum.PATIENT));
    model.addAttribute("prescriptionStatuses", PrescriptionStatusEnum.values());
    model.addAttribute("patientId", patientId);
    model.addAttribute("status", status);
    model.addAttribute("startDate", startDate);
    model.addAttribute("endDate", endDate);
    model.addAttribute("remainingRefills", remainingRefills);

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
        ensurePrescriptionHasMedicineRow(request);
        model.addAttribute("request", request);
        model.addAttribute("errorMessage", e.getMessage());
        model.addAttribute("minPrescriptionDate", LocalDate.now());
        return "doctor/dashboard/create-prescription";
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
    ensurePrescriptionHasMedicineRow(request);

    model.addAttribute("request", request);
    model.addAttribute("minPrescriptionDate", LocalDate.now());

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
    ensureLabRequestHasTestRow(request);

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
        ensureLabRequestHasTestRow(request);
        model.addAttribute("request", request);
        model.addAttribute("errorMessage", e.getMessage());
        return "doctor/dashboard/create-lab";
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

    private List<Appointment> filterAppointmentsByDateRange(
        List<Appointment> appointments,
        LocalDate from,
        LocalDate end,
        String sectionName
    ) {
        if (from != null && end != null && from.isAfter(end)) {
            throw new IllegalArgumentException(sectionName + " start date must be on or before end date");
        }

        if (from == null && end == null) {
            return appointments;
        }

        return appointments.stream()
            .filter(appointment -> {
                LocalDate appointmentDate = appointment.getStartTime().toLocalDate();
                return (from == null || !appointmentDate.isBefore(from))
                    && (end == null || !appointmentDate.isAfter(end));
            })
            .toList();
    }

    private List<Precheck> filterPrechecksForDoctor(
        List<Precheck> prechecks,
        LocalDate from,
        LocalDate end,
        AppointmentTypeEnum type
    ) {
        if (from != null && end != null && from.isAfter(end)) {
            throw new IllegalArgumentException("start date must be on or before end date");
        }

        return prechecks.stream()
            .filter(precheck -> {
                LocalDate createdAtDate = precheck.getCreatedAt().toLocalDate();
                boolean withinDateRange =
                    (from == null || !createdAtDate.isBefore(from))
                    && (end == null || !createdAtDate.isAfter(end));

                boolean matchesType =
                    type == null
                    || (precheck.getAppointment() != null && precheck.getAppointment().getType() == type);

                return withinDateRange && matchesType;
            })
            .toList();
    }

    private void ensurePrescriptionHasMedicineRow(CreatePrescriptionRequest request) {
        if (request.getMedicines() == null) {
            request.setMedicines(new ArrayList<>());
        }

        if (request.getMedicines().isEmpty()) {
            request.getMedicines().add(new CreatePrescriptionMedicineRequest());
        }
    }

    private void ensureLabRequestHasTestRow(CreateLabRequestRequest request) {
        if (request.getLabTests() == null) {
            request.setLabTests(new ArrayList<>());
        }

        if (request.getLabTests().isEmpty()) {
            request.getLabTests().add(new CreateLabRequestRequest.LabTestRequest());
        }
    }

    private User requireActiveUser(OidcUser oidcUser) {
        UUID userId = OidcUserUtil.getUserId(oidcUser);
        return userRepository.findByIdAndDeletedFalse(userId)
            .orElseThrow();
    }
}
