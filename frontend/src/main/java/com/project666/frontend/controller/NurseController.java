package com.project666.frontend.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.project666.backend.domain.CreatePrecheckRequest;
import com.project666.backend.domain.ListAppointmentRequest;
import com.project666.backend.domain.ListPrecheckRequest;
import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.entity.Precheck;
import com.project666.backend.domain.entity.PrecheckStatusEnum;
import com.project666.backend.domain.entity.RoleEnum;
import com.project666.backend.domain.entity.User;
import com.project666.backend.repository.UserRepository;
import com.project666.backend.service.AppointmentService;
import com.project666.backend.service.PrecheckService;
import com.project666.frontend.service.KeycloakService;
import com.project666.frontend.util.OidcUserUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/nurse")
@PreAuthorize("hasRole('NURSE')")
public class NurseController {

    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final AppointmentService appointmentService;
    private final PrecheckService precheckService;


    @GetMapping("/dashboard/home")
    public String home(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
        @RequestParam(required = false) UUID filterPatientId,
        @RequestParam(required = false) UUID filterDoctorId,
        Model model
    ) {
        User user = requireActiveUser(oidcUser);
        keycloakService.syncUser(authorizedClient, user);
        userRepository.save(user);

        UUID nurseId = user.getId();
        LocalDate today = LocalDate.now();
        Pageable pageable = PageRequest.of(0, 200, Sort.by("startTime").ascending());
        ListAppointmentRequest request = new ListAppointmentRequest();
        request.setPatientId(filterPatientId);
        request.setDoctorId(filterDoctorId);
        request.setFrom(today);
        request.setEnd(today);
        List<Appointment> appointments = appointmentService
            .listAppointmentForNurse(nurseId, request, pageable)
            .getContent();

        List<User> patients = appointments.stream()
            .map(Appointment::getPatient)
            .distinct()
            .sorted(Comparator.comparing(u -> u.getFirstName() + u.getLastName()))
            .collect(Collectors.toList());

        List<User> doctors = appointments.stream()
            .map(Appointment::getDoctor)
            .distinct()
            .sorted(Comparator.comparing(u -> u.getFirstName() + u.getLastName()))
            .collect(Collectors.toList());

        Map<UUID, Precheck> precheckByAppointment = appointments.stream()
            .flatMap(a -> a.getPrechecks().stream())
            .filter(p -> PrecheckStatusEnum.VALID.equals(p.getStatus()) && nurseId.equals(p.getNurse().getId()))
            .collect(Collectors.toMap(p -> p.getAppointment().getId(), p -> p));

        Map<String, List<Map<String, Object>>> historyByPatient = new LinkedHashMap<>();
        for (Appointment a : appointments) {
            String patientId = a.getPatient().getId().toString();
            if (!historyByPatient.containsKey(patientId)) {
                ListPrecheckRequest histReq = new ListPrecheckRequest();
                histReq.setStatus(PrecheckStatusEnum.VALID);
                List<Precheck> history = precheckService.listPrecheckForPatient(
                    a.getPatient().getId(), histReq, PageRequest.of(0, 50, Sort.by("createdAt").descending())
                ).getContent();
                List<Map<String, Object>> historyData = history.stream().map(p -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("pulse", p.getPulse());
                    entry.put("sugar", p.getSugar());
                    entry.put("temperature", p.getTemperature());
                    entry.put("height", p.getHeight());
                    entry.put("weight", p.getWeight());
                    entry.put("note", p.getNote() != null ? p.getNote() : "");
                    entry.put("createdAt", p.getCreatedAt().toLocalDate().toString());
                    entry.put("nurseName", p.getNurse().getFirstName() + " " + p.getNurse().getLastName());
                    return entry;
                }).collect(Collectors.toList());
                historyByPatient.put(patientId, historyData);
            }
        }

        String filterPatientName = filterPatientId == null ? "" : patients.stream()
            .filter(u -> filterPatientId.equals(u.getId()))
            .findFirst()
            .map(u -> u.getFirstName() + " " + u.getLastName())
            .orElse("");

        String filterDoctorName = filterDoctorId == null ? "" : doctors.stream()
            .filter(u -> filterDoctorId.equals(u.getId()))
            .findFirst()
            .map(u -> u.getFirstName() + " " + u.getLastName())
            .orElse("");

        model.addAttribute("appointments", appointments);
        model.addAttribute("precheckByAppointment", precheckByAppointment);
        model.addAttribute("historyByPatient", historyByPatient);
        model.addAttribute("patients", patients);
        model.addAttribute("doctors", doctors);
        model.addAttribute("filterPatientId", filterPatientId);
        model.addAttribute("filterDoctorId", filterDoctorId);
        model.addAttribute("filterPatientName", filterPatientName);
        model.addAttribute("filterDoctorName", filterDoctorName);
        model.addAttribute("today", today);
        return "nurse/dashboard/home";
    }


    @PostMapping("/dashboard/submit-precheck")
    public String submitPrecheck(
        @RequestParam UUID appointmentId,
        @RequestParam Integer pulse,
        @RequestParam Double sugar,
        @RequestParam Double temperature,
        @RequestParam Double height,
        @RequestParam Double weight,
        @RequestParam(required = false) String note,
        @AuthenticationPrincipal OidcUser oidcUser,
        RedirectAttributes redirectAttributes
    ) {
        UUID nurseId = OidcUserUtil.getUserId(oidcUser);

        CreatePrecheckRequest request = new CreatePrecheckRequest();
        request.setAppointmentId(appointmentId);
        request.setPulse(pulse);
        request.setSugar(sugar);
        request.setTemperature(temperature);
        request.setHeight(height);
        request.setWeight(weight);
        request.setNote(note);

        try {
            precheckService.createPrecheck(nurseId, request);
            return "redirect:/nurse/dashboard/home?success";
        } catch (Exception e) {
            log.error("Failed to create precheck for appointment {}: {}", appointmentId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", resolveErrorMessage(e, "Failed to submit precheck."));
            return "redirect:/nurse/dashboard/home";
        }
    }

    @PostMapping("/dashboard/cancel-precheck")
    public String cancelPrecheck(
        @RequestParam UUID precheckId,
        @AuthenticationPrincipal OidcUser oidcUser,
        RedirectAttributes redirectAttributes
    ) {
        UUID nurseId = OidcUserUtil.getUserId(oidcUser);
        try {
            precheckService.cancelPrecheck(nurseId, precheckId);
            return "redirect:/nurse/dashboard/home?cancelled";
        } catch (Exception e) {
            log.error("Failed to cancel precheck {}: {}", precheckId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", resolveErrorMessage(e, "Failed to cancel precheck."));
            return "redirect:/nurse/dashboard/home";
        }
    }

    @GetMapping("/dashboard/profile")
    public String profile(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
        Model model
    ) {
        User user = requireActiveUser(oidcUser);
        keycloakService.syncUser(authorizedClient, user);
        userRepository.save(user);
        model.addAttribute("user", user);
        return "nurse/dashboard/profile";
    }

    @GetMapping("/dashboard/prechecks")
    public String prechecks(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RequestParam(required = false) LocalDate from,
        @RequestParam(required = false) LocalDate end,
        @RequestParam(required = false) UUID patientId,
        @RequestParam(required = false) PrecheckStatusEnum status,
        Model model
    ) {
        User user = requireActiveUser(oidcUser);
        UUID nurseId = user.getId();

        ListPrecheckRequest request = new ListPrecheckRequest();
        request.setPatientId(patientId);
        request.setStatus(status);
        request.setMinDate(from);
        request.setMaxDate(end);

        Pageable pageable = PageRequest.of(0, 1000, Sort.by("createdAt").descending());

        List<Precheck> prechecks = new ArrayList<>();
        String filterError = null;
        try {
            prechecks = precheckService.listPrecheckForNurse(nurseId, request, pageable).getContent();
        } catch (IllegalArgumentException e) {
            filterError = e.getMessage();
        }

        model.addAttribute("prechecks", prechecks);
        model.addAttribute("from", from);
        model.addAttribute("end", end);
        model.addAttribute("patientId", patientId);
        model.addAttribute("status", status);
        model.addAttribute("filterError", filterError);
        model.addAttribute("patients", userRepository.findAllByRoleAndDeletedFalse(RoleEnum.PATIENT));
        model.addAttribute("precheckStatuses", PrecheckStatusEnum.values());

        return "nurse/dashboard/prechecks";
    }

    @PostMapping("/delete-account")
    public String deleteAccount(@AuthenticationPrincipal OidcUser oidcUser) {
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
        return userRepository.findByIdAndDeletedFalse(userId).orElseThrow();
    }

    private String resolveErrorMessage(Exception exception, String fallback) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }
}
