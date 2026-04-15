package com.project666.frontend.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
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

import com.project666.backend.domain.ListAppointmentBillRequest;
import com.project666.backend.domain.ListLabBillRequest;
import com.project666.backend.domain.entity.AppointmentBill;
import com.project666.backend.domain.entity.BillStatusEnum;
import com.project666.backend.domain.entity.LabBill;
import com.project666.backend.domain.entity.RoleEnum;
import com.project666.backend.domain.entity.User;
import com.project666.backend.repository.UserRepository;
import com.project666.backend.service.BillService;
import com.project666.frontend.service.KeycloakService;
import com.project666.frontend.util.OidcUserUtil;

import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
@RequestMapping("/accountant")
@PreAuthorize("hasRole('ACCOUNTANT')")
public class AccountantController {

    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final BillService billService;

    @GetMapping("/homepage")
    public String redirectHomepage() {
        return "redirect:/accountant/dashboard/home";
    }

    @GetMapping("/dashboard/home")
    public String home(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
        @RequestParam(required = false) LocalDate appointmentFrom,
        @RequestParam(required = false) LocalDate appointmentEnd,
        @RequestParam(required = false) BigDecimal appointmentMinAmount,
        @RequestParam(required = false) BigDecimal appointmentMaxAmount,
        @RequestParam(required = false) String appointmentPatient,
        @RequestParam(required = false) BillStatusEnum appointmentStatus,
        @RequestParam(required = false) LocalDate labFrom,
        @RequestParam(required = false) LocalDate labEnd,
        @RequestParam(required = false) BigDecimal labMinAmount,
        @RequestParam(required = false) BigDecimal labMaxAmount,
        @RequestParam(required = false) String labPatient,
        @RequestParam(required = false) BillStatusEnum labStatus,
        @RequestParam(required = false, defaultValue = "appointment") String activeTab,
        Model model
    ) {
        User user = requireActiveUser(oidcUser);
        keycloakService.syncUser(authorizedClient, user);
        userRepository.save(user);
        UUID accountantId = user.getId();

        Pageable billPageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        List<AppointmentBill> appointmentBills = new ArrayList<>();
        List<LabBill> labBills = new ArrayList<>();
        String appointmentBillError = null;
        String labBillError = null;

        try {
            ListAppointmentBillRequest apptRequest = new ListAppointmentBillRequest();
            apptRequest.setFrom(appointmentFrom);
            apptRequest.setEnd(appointmentEnd);
            apptRequest.setMinAmount(appointmentMinAmount);
            apptRequest.setMaxAmount(appointmentMaxAmount);
            apptRequest.setPatientId(parseUuidOrNull(appointmentPatient));
            apptRequest.setStatus(appointmentStatus);

            Page<AppointmentBill> apptBills =
                billService.searchAnyAppointmentBillForAccountant(accountantId, apptRequest, billPageable);
            appointmentBills = apptBills.getContent();
        } catch (IllegalArgumentException e) {
            appointmentBillError = e.getMessage();
        }

        try {
            ListLabBillRequest labRequest = new ListLabBillRequest();
            labRequest.setFrom(labFrom);
            labRequest.setEnd(labEnd);
            labRequest.setMinAmount(labMinAmount);
            labRequest.setMaxAmount(labMaxAmount);
            labRequest.setPatientId(parseUuidOrNull(labPatient));
            labRequest.setStatus(labStatus);

            Page<LabBill> labBillsPage =
                billService.searchAnyLabBillForAccountant(accountantId, labRequest, billPageable);
            labBills = labBillsPage.getContent();
        } catch (IllegalArgumentException e) {
            labBillError = e.getMessage();
        }

        model.addAttribute("appointmentBills", appointmentBills);
        model.addAttribute("labBills", labBills);
        model.addAttribute("appointmentFrom", appointmentFrom);
        model.addAttribute("appointmentEnd", appointmentEnd);
        model.addAttribute("appointmentMinAmount", appointmentMinAmount);
        model.addAttribute("appointmentMaxAmount", appointmentMaxAmount);
        model.addAttribute("appointmentPatient", appointmentPatient);
        model.addAttribute("appointmentStatus", appointmentStatus);
        model.addAttribute("labFrom", labFrom);
        model.addAttribute("labEnd", labEnd);
        model.addAttribute("labMinAmount", labMinAmount);
        model.addAttribute("labMaxAmount", labMaxAmount);
        model.addAttribute("labPatient", labPatient);
        model.addAttribute("labStatus", labStatus);
        model.addAttribute("billStatuses", BillStatusEnum.values());
        model.addAttribute("patients", userRepository.findAllByRoleAndDeletedFalse(RoleEnum.PATIENT));
        model.addAttribute("appointmentBillError", appointmentBillError);
        model.addAttribute("labBillError", labBillError);
        model.addAttribute("activeTab", "lab".equalsIgnoreCase(activeTab) ? "lab" : "appointment");

        return "accountant/dashboard/home";
    }

    @PostMapping("/dashboard/confirm-payment")
    public String confirmPayment(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RequestParam UUID billId,
        @RequestParam(required = false) String appointmentStatus,
        @RequestParam(required = false) String labStatus,
        @RequestParam(required = false) String activeTab,
        RedirectAttributes redirectAttributes
    ) {
        UUID accountantId = requireActiveUser(oidcUser).getId();

        try {
            billService.confirmBillPayment(accountantId, billId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        StringBuilder redirectUrl = new StringBuilder("redirect:/accountant/dashboard/home");
        List<String> params = new ArrayList<>();
        if (appointmentStatus != null && !appointmentStatus.isBlank()) {
            params.add("appointmentStatus=" + appointmentStatus);
        }
        if (labStatus != null && !labStatus.isBlank()) {
            params.add("labStatus=" + labStatus);
        }
        if (activeTab != null && !activeTab.isBlank()) {
            params.add("activeTab=" + activeTab);
        }
        if (!params.isEmpty()) {
            redirectUrl.append("?").append(String.join("&", params));
        }

        return redirectUrl.toString();
    }

    @GetMapping("/dashboard/notifications")
    public String notifications() {
        return "accountant/dashboard/notifications";
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
        return "accountant/dashboard/profile";
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

    private BillStatusEnum resolveStatus(String tab) {
        return switch (tab) {
            case "viewing" -> BillStatusEnum.VIEWING;
            case "paid"    -> BillStatusEnum.PAID;
            case "unpaid"  -> BillStatusEnum.UNPAID;
            default        -> null; // "all" — no status filter
        };
    }

    private String normalizeStatusFilter(String status) {
        return (status == null || status.isBlank()) ? "all" : status.toLowerCase();
    }

    private UUID parseUuidOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value.trim());
    }

    private User requireActiveUser(OidcUser oidcUser) {
        UUID userId = OidcUserUtil.getUserId(oidcUser);
        return userRepository.findByIdAndDeletedFalse(userId)
            .orElseThrow();
    }
}
