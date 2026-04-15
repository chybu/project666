package com.project666.frontend.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.entity.AppointmentBill;
import com.project666.backend.domain.entity.AppointmentStatusEnum;
import com.project666.backend.domain.entity.BillStatusEnum;
import com.project666.backend.domain.entity.LabBill;
import com.project666.backend.domain.entity.User;
import com.project666.backend.repository.AppointmentRepository;
import com.project666.backend.repository.UserRepository;
import com.project666.backend.service.BillService;
import com.project666.backend.specification.AppointmentSpecification;
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
    private final AppointmentRepository appointmentRepository;

    @GetMapping("/homepage")
    public String redirectHomepage() {
        return "redirect:/accountant/dashboard/home";
    }

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
        keycloakService.syncUser(authorizedClient, user);
        userRepository.save(user);

        LocalDate today = LocalDate.now();
        int selectedYear = (year != null) ? year : today.getYear();
        int selectedMonth = (month != null) ? month : today.getMonthValue();

        YearMonth yearMonth = YearMonth.of(selectedYear, selectedMonth);
        LocalDate firstDayOfMonth = yearMonth.atDay(1);
        LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();

        Specification<Appointment> spec = AppointmentSpecification.alwaysTrue()
            .and(AppointmentSpecification.byStatus(AppointmentStatusEnum.CONFIRMED))
            .and(AppointmentSpecification.byDateRange(
                firstDayOfMonth.atStartOfDay(),
                lastDayOfMonth.atTime(LocalTime.MAX)
            ));

        Pageable pageable = PageRequest.of(0, 100, Sort.by("startTime").ascending());
        List<Appointment> monthAppointments = appointmentRepository.findAll(spec, pageable).getContent();

        Map<LocalDate, List<Appointment>> appointmentsByDate = new HashMap<>();
        for (Appointment appointment : monthAppointments) {
            LocalDate date = appointment.getStartTime().toLocalDate();
            appointmentsByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(appointment);
        }

        List<LocalDate> calendarDays = new ArrayList<>();
        int leadingBlanks = firstDayOfMonth.getDayOfWeek().getValue() % 7;
        for (int i = 0; i < leadingBlanks; i++) calendarDays.add(null);
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) calendarDays.add(yearMonth.atDay(day));
        while (calendarDays.size() % 7 != 0) calendarDays.add(null);

        YearMonth prevMonth = yearMonth.minusMonths(1);
        YearMonth nextMonth = yearMonth.plusMonths(1);

        List<Appointment> selectedDayAppointments =
            (selectedDate != null && appointmentsByDate.containsKey(selectedDate))
                ? appointmentsByDate.get(selectedDate)
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
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("selectedDayAppointments", selectedDayAppointments);

        return "accountant/dashboard/home";
    }

    @GetMapping("/dashboard/finances")
    public String finances(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RequestParam(required = false, defaultValue = "all") String apptTab,
        @RequestParam(required = false, defaultValue = "all") String labTab,
        Model model
    ) {
        UUID accountantId = requireActiveUser(oidcUser).getId();
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());

        // "all" leaves status null so no status filter is applied
        BillStatusEnum apptStatus = resolveStatus(apptTab);
        ListAppointmentBillRequest apptRequest = new ListAppointmentBillRequest();
        apptRequest.setStatus(apptStatus);
        Page<AppointmentBill> apptBills = "paid".equals(apptTab)
            ? billService.listAppointmentBillForAccountant(accountantId, apptRequest, pageable)
            : billService.searchAnyAppointmentBillForAccountant(accountantId, apptRequest, pageable);

        BillStatusEnum labStatus = resolveStatus(labTab);
        ListLabBillRequest labRequest = new ListLabBillRequest();
        labRequest.setStatus(labStatus);
        Page<LabBill> labBills = "paid".equals(labTab)
            ? billService.listLabBillForAccountant(accountantId, labRequest, pageable)
            : billService.searchAnyLabBillForAccountant(accountantId, labRequest, pageable);

        model.addAttribute("appointmentBills", apptBills.getContent());
        model.addAttribute("labBills", labBills.getContent());
        model.addAttribute("apptTab", apptTab);
        model.addAttribute("labTab", labTab);

        return "accountant/dashboard/finances";
    }

    @PostMapping("/dashboard/confirm-payment")
    public String confirmPayment(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RequestParam UUID billId,
        @RequestParam(defaultValue = "unpaid") String apptTab,
        @RequestParam(defaultValue = "unpaid") String labTab,
        RedirectAttributes redirectAttributes
    ) {
        UUID accountantId = requireActiveUser(oidcUser).getId();

        try {
            billService.confirmBillPayment(accountantId, billId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/accountant/dashboard/finances?apptTab=" + apptTab + "&labTab=" + labTab;
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

    private User requireActiveUser(OidcUser oidcUser) {
        UUID userId = OidcUserUtil.getUserId(oidcUser);
        return userRepository.findByIdAndDeletedFalse(userId)
            .orElseThrow();
    }
}
