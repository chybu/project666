package com.project666.frontend.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.project666.backend.domain.dto.PatientLabRequestResponseDto;
import com.project666.backend.domain.entity.LabRequest;
import com.project666.backend.domain.entity.Prescription;
import com.project666.backend.mapper.LabMapper;
import com.project666.backend.service.LabService;
import com.project666.backend.service.PrescriptionService;
import com.project666.frontend.service.PdfExportService;
import com.project666.frontend.util.OidcUserUtil;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/pdf")
public class PdfController {

    private final PrescriptionService prescriptionService;
    private final LabService labService;
    private final LabMapper labMapper;
    private final PdfExportService pdfExportService;

    @GetMapping("/prescription/patient")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<byte[]> exportPatientPrescriptionPdf(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RequestParam UUID prescriptionId
    ) {
        UUID patientId = OidcUserUtil.getUserId(oidcUser);
        Prescription prescription = prescriptionService.getPrescriptionForPatient(patientId, prescriptionId);

        return buildPrescriptionPdfResponse(prescription, "patient", oidcUser);
    }

    @GetMapping("/prescription/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<byte[]> exportDoctorPrescriptionPdf(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RequestParam UUID prescriptionId
    ) {
        UUID doctorId = OidcUserUtil.getUserId(oidcUser);
        Prescription prescription = prescriptionService.getPrescriptionForDoctor(doctorId, prescriptionId);

        return buildPrescriptionPdfResponse(prescription, "doctor", oidcUser);
    }

    @GetMapping("/prescription/doctor/shared")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<byte[]> exportSharedDoctorPrescriptionPdf(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RequestParam UUID prescriptionId
    ) {
        UUID doctorId = OidcUserUtil.getUserId(oidcUser);
        Prescription prescription = prescriptionService.getSharedPrescriptionForDoctor(doctorId, prescriptionId);

        return buildPrescriptionPdfResponse(prescription, "doctor", oidcUser);
    }

    private ResponseEntity<byte[]> buildPrescriptionPdfResponse(
        Prescription prescription,
        String viewerRole,
        OidcUser oidcUser
    ) {
        LocalDateTime printedAt = LocalDateTime.now();
        Map<String, Object> variables = new HashMap<>();
        variables.put("prescription", prescription);
        variables.put("viewerRole", viewerRole);
        variables.put("generatedAt", printedAt);
        variables.put("printedAt", printedAt);
        variables.put("printedBy", oidcUser.getFullName() != null ? oidcUser.getFullName() : oidcUser.getPreferredUsername());
        variables.put("printedByRole", viewerRole != null ? viewerRole.toUpperCase() : "UNKNOWN");
        variables.put("printedByUserId", OidcUserUtil.getUserId(oidcUser));

        byte[] pdf = pdfExportService.render("pdf/prescription-detail", variables);

        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=prescription-" + prescription.getId() + ".pdf"
            )
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    @GetMapping("/lab/patient")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<byte[]> exportPatientLabPdf(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RequestParam UUID labId
    ) {
        UUID patientId = OidcUserUtil.getUserId(oidcUser);
        LabRequest labRequest = labService.getLabRequestForPatient(patientId, labId);
        PatientLabRequestResponseDto patientLabRequest = labMapper.toPatientLabRequestResponseDto(labRequest);

        return buildPatientLabPdfResponse(patientLabRequest, oidcUser);
    }

    @GetMapping("/lab/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<byte[]> exportDoctorLabPdf(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RequestParam UUID labId
    ) {
        UUID doctorId = OidcUserUtil.getUserId(oidcUser);
        LabRequest labRequest = labService.getLabRequestForDoctor(doctorId, labId);

        return buildLabPdfResponse(labRequest, "doctor", oidcUser);
    }

    @GetMapping("/lab/doctor/shared")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<byte[]> exportSharedDoctorLabPdf(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RequestParam UUID labId
    ) {
        UUID doctorId = OidcUserUtil.getUserId(oidcUser);
        LabRequest labRequest = labService.getSharedLabRequestForDoctor(doctorId, labId);

        return buildLabPdfResponse(labRequest, "doctor", oidcUser);
    }

    private ResponseEntity<byte[]> buildLabPdfResponse(
        LabRequest labRequest,
        String viewerRole,
        OidcUser oidcUser
    ) {
        LocalDateTime printedAt = LocalDateTime.now();
        Map<String, Object> variables = new HashMap<>();
        variables.put("lab", labRequest);
        variables.put("viewerRole", viewerRole);
        variables.put("generatedAt", printedAt);
        variables.put("printedAt", printedAt);
        variables.put("printedBy", oidcUser.getFullName() != null ? oidcUser.getFullName() : oidcUser.getPreferredUsername());
        variables.put("printedByRole", viewerRole != null ? viewerRole.toUpperCase() : "UNKNOWN");
        variables.put("printedByUserId", OidcUserUtil.getUserId(oidcUser));

        byte[] pdf = pdfExportService.render("pdf/lab-request-detail", variables);

        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=lab-request-" + labRequest.getId() + ".pdf"
            )
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    private ResponseEntity<byte[]> buildPatientLabPdfResponse(
        PatientLabRequestResponseDto labRequest,
        OidcUser oidcUser
    ) {
        LocalDateTime printedAt = LocalDateTime.now();
        Map<String, Object> variables = new HashMap<>();
        variables.put("lab", labRequest);
        variables.put("viewerRole", "patient");
        variables.put("generatedAt", printedAt);
        variables.put("printedAt", printedAt);
        variables.put("printedBy", oidcUser.getFullName() != null ? oidcUser.getFullName() : oidcUser.getPreferredUsername());
        variables.put("printedByRole", "PATIENT");
        variables.put("printedByUserId", OidcUserUtil.getUserId(oidcUser));

        byte[] pdf = pdfExportService.render("pdf/lab-request-detail-patient", variables);

        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=lab-request-" + labRequest.getId() + ".pdf"
            )
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
}
