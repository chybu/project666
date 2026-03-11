package demo.controller.client;

import java.time.LocalDate;
import java.util.UUID;

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
import org.springframework.web.bind.annotation.RequestMapping;

import demo.domain.ListAppointmentRequest;
import demo.domain.entities.Appointment;
import demo.domain.entities.AppointmentStatusEnum;
import demo.service.AppointmentService;
import demo.util.OidcUserUtil;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/patient")
public class PatientController {

    private final AppointmentService appointmentService;

    @GetMapping("/homepage")
    @PreAuthorize("hasRole('PATIENT')")
    public String loadHomePage(
        @AuthenticationPrincipal OidcUser oidcUser,
        Model model
    ){
        ListAppointmentRequest request = new ListAppointmentRequest();
        request.setStatus(AppointmentStatusEnum.CONFIRMED);
        request.setFrom(LocalDate.now());

        UUID patientId = OidcUserUtil.getUserId(oidcUser);

        Pageable pageable = PageRequest.of(0, 5, Sort.by("startTime").ascending());

        Page<Appointment> appointmentPage = appointmentService.listDoctorAppointment(patientId, request, pageable);

        model.addAttribute("appointments", appointmentPage.getContent());

        return "patient/homepage";
    }
}