package com.project666.frontend.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.project666.backend.domain.entity.User;
import com.project666.backend.repository.UserRepository;
import com.project666.backend.service.LabService;
import com.project666.frontend.service.KeycloakService;
import com.project666.frontend.util.OidcUserUtil;


import com.project666.backend.domain.ListLabRequestRequest;
import com.project666.backend.domain.ListLabTestRequest;
import com.project666.backend.domain.UpdateLabTestRequest;
import com.project666.backend.domain.entity.LabTestStatusEnum;
import com.project666.backend.domain.entity.RoleEnum;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/labtechnician")
@PreAuthorize("hasRole('LAB_TECHNICIAN')")
public class LabTechnicianController {

    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final LabService labService;

    @GetMapping("/homepage")
    public String redirectHomepage() {
        return "redirect:/labtechnician/dashboard/home";
    }

    @GetMapping("/dashboard/home")
    public String home(
            @AuthenticationPrincipal OidcUser oidcUser,
            @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) LocalDate minDate,
            @RequestParam(required = false) LocalDate maxDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        User user = requireActiveUser(oidcUser);

        keycloakService.syncUser(authorizedClient, user);
        userRepository.save(user);

        Pageable pageable = PageRequest.of(page, size);
        Page<?> labRequestsPage = Page.empty(pageable);
        String filterError = null;

        ListLabRequestRequest request = new ListLabRequestRequest();
        request.setPatientId(patientId);
        request.setDoctorId(doctorId);
        request.setMinDate(minDate);
        request.setMaxDate(maxDate);

        try {
            labRequestsPage = labService.listLabRequestForLabTechnician(user.getId(), request, pageable);
        } catch (IllegalArgumentException e) {
            filterError = e.getMessage();
        }

        model.addAttribute("user", user);
        model.addAttribute("labRequestsPage", labRequestsPage);
        model.addAttribute("patientId", patientId);
        model.addAttribute("doctorId", doctorId);
        model.addAttribute("minDate", minDate);
        model.addAttribute("maxDate", maxDate);
        model.addAttribute("filterError", filterError);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("patients",
                userRepository.findAllByRoleAndDeletedFalse(RoleEnum.PATIENT));

        model.addAttribute("doctors",
                userRepository.findAllByRoleAndDeletedFalse(RoleEnum.DOCTOR));

        return "labtechnician/dashboard/home";
    }

    @PostMapping("/dashboard/lab-tests/{labTestId}/claim")
    public String claimLabTest(
            @AuthenticationPrincipal OidcUser oidcUser,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) LocalDate minDate,
            @RequestParam(required = false) LocalDate maxDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @org.springframework.web.bind.annotation.PathVariable UUID labTestId,
            RedirectAttributes redirectAttributes
    ) {
        User user = requireActiveUser(oidcUser);

        try {
            labService.claimLabTest(user.getId(), labTestId);
            redirectAttributes.addFlashAttribute("successMessage", "Lab test claimed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/labtechnician/dashboard/home"
                + buildLabRequestQueryString(patientId, doctorId, minDate, maxDate, page, size);
    }

    @GetMapping("/dashboard/lab-tests")
    public String labTests(
            @AuthenticationPrincipal OidcUser oidcUser,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) LocalDate minDate,
            @RequestParam(required = false) LocalDate maxDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        User user = requireActiveUser(oidcUser);
        String effectiveStatus = (status != null && !status.isBlank()) ? status : LabTestStatusEnum.IN_PROGRESS.name();

        Pageable pageable = PageRequest.of(page, size);
        Page<?> labTestsPage = Page.empty(pageable);
        String filterError = null;

        ListLabTestRequest request = new ListLabTestRequest();
        request.setPatientId(patientId);
        request.setDoctorId(doctorId);
        request.setCode(code);
        request.setName(name);
        request.setUnit(unit);
        request.setMinDate(minDate);
        request.setMaxDate(maxDate);

        request.setStatus(LabTestStatusEnum.valueOf(effectiveStatus));

        try {
            labTestsPage = labService.listLabTestForLabTechnician(user.getId(), request, pageable);
        } catch (IllegalArgumentException e) {
            filterError = e.getMessage();
        }

        model.addAttribute("labTestsPage", labTestsPage);
        model.addAttribute("patientId", patientId);
        model.addAttribute("doctorId", doctorId);
        model.addAttribute("status", effectiveStatus);
        model.addAttribute("code", code);
        model.addAttribute("name", name);
        model.addAttribute("unit", unit);
        model.addAttribute("minDate", minDate);
        model.addAttribute("maxDate", maxDate);
        model.addAttribute("filterError", filterError);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("patients",
                userRepository.findAllByRoleAndDeletedFalse(RoleEnum.PATIENT));

        model.addAttribute("doctors",
                userRepository.findAllByRoleAndDeletedFalse(RoleEnum.DOCTOR));

        return "labtechnician/dashboard/lab-tests";
    }

    @PostMapping("/dashboard/lab-tests/update")
    public String updateLabTest(
            @AuthenticationPrincipal OidcUser oidcUser,
            @RequestParam UUID labTestId,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String labTechnicianNote,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String filterCode,
            @RequestParam(required = false) String filterName,
            @RequestParam(required = false) String filterUnit,
            @RequestParam(required = false) LocalDate minDate,
            @RequestParam(required = false) LocalDate maxDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            RedirectAttributes redirectAttributes
    ) {
        User user = requireActiveUser(oidcUser);

        try {
            UpdateLabTestRequest request = new UpdateLabTestRequest();
            request.setLabTestId(labTestId);
            request.setCode(code);
            request.setName(name);
            request.setUnit(unit);
            request.setResult(result);
            request.setLabTechnicianNote(labTechnicianNote);

            labService.updateLabTest(user.getId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Lab test updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/labtechnician/dashboard/lab-tests"
                + buildLabTestQueryString(patientId, doctorId, status, filterCode, filterName, filterUnit, minDate, maxDate, page, size);
    }

    @PostMapping("/dashboard/lab-tests/{labTestId}/submit")
    public String submitLabTest(
            @AuthenticationPrincipal OidcUser oidcUser,
            @org.springframework.web.bind.annotation.PathVariable UUID labTestId,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) LocalDate minDate,
            @RequestParam(required = false) LocalDate maxDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            RedirectAttributes redirectAttributes
    ) {
        User user = requireActiveUser(oidcUser);

        try {
            labService.submitLabTest(user.getId(), labTestId);
            redirectAttributes.addFlashAttribute("successMessage", "Lab test submitted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/labtechnician/dashboard/lab-tests"
                + buildLabTestQueryString(patientId, doctorId, status, code, name, unit, minDate, maxDate, page, size);
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
        return "labtechnician/dashboard/profile";
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

    private String buildLabRequestQueryString(
            UUID patientId,
            UUID doctorId,
            LocalDate minDate,
            LocalDate maxDate,
            int page,
            int size
    ) {
        StringBuilder sb = new StringBuilder("?");
        if (patientId != null) sb.append("patientId=").append(patientId).append("&");
        if (doctorId != null) sb.append("doctorId=").append(doctorId).append("&");
        if (minDate != null) sb.append("minDate=").append(minDate).append("&");
        if (maxDate != null) sb.append("maxDate=").append(maxDate).append("&");
        sb.append("page=").append(page).append("&size=").append(size);
        return sb.toString();
    }

    private String buildLabTestQueryString(
            UUID patientId,
            UUID doctorId,
            String status,
            String code,
            String name,
            String unit,
            LocalDate minDate,
            LocalDate maxDate,
            int page,
            int size
    ) {
        StringBuilder sb = new StringBuilder("?");
        if (patientId != null) sb.append("patientId=").append(patientId).append("&");
        if (doctorId != null) sb.append("doctorId=").append(doctorId).append("&");
        if (status != null && !status.isBlank()) sb.append("status=").append(status).append("&");
        if (code != null && !code.isBlank()) sb.append("code=").append(code).append("&");
        if (name != null && !name.isBlank()) sb.append("name=").append(name).append("&");
        if (unit != null && !unit.isBlank()) sb.append("unit=").append(unit).append("&");
        if (minDate != null) sb.append("minDate=").append(minDate).append("&");
        if (maxDate != null) sb.append("maxDate=").append(maxDate).append("&");
        sb.append("page=").append(page).append("&size=").append(size);
        return sb.toString();
    }
}
