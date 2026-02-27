package demo.controllers;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.domains.CancelAppointmentRequest;
import demo.domains.CreateAppointmentRequest;
import demo.domains.ListAppointmentRequest;
import demo.domains.dtos.CancelAppointmentRequestDto;
import demo.domains.dtos.CancelAppointmentResponseDto;
import demo.domains.dtos.ConfirmAppointmentResponseDto;
import demo.domains.dtos.CreateAppointmentRequestDto;
import demo.domains.dtos.CreateAppointmentResponseDto;
import demo.domains.dtos.ListAppointmentRequestDto;
import demo.domains.dtos.ListAppointmentResponseDto;
import demo.domains.entities.Appointment;
import demo.domains.entities.RoleEnum;
import demo.exceptions.RoleNotFoundException;
import demo.mappers.AppointmentMapper;
import demo.services.AppointmentService;
import demo.utils.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentMapper appointmentMapper;
    private final AppointmentService appointmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('PATIENT', 'RECEPTIONIST')")
    public ResponseEntity<CreateAppointmentResponseDto> createAppointment(
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody @Valid CreateAppointmentRequestDto requestDto
    ){
        CreateAppointmentRequest request = appointmentMapper.fromCreateAppointmentRequesDto(requestDto);
        UUID creatorId = JwtUtil.getUserId(jwt);

        Appointment createdAppointment = appointmentService.createAppointment(creatorId, request);
        CreateAppointmentResponseDto responseDto = appointmentMapper.toCreateAppointmentResponseDto(createdAppointment);

        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/{appointmentId}/confirm")
    @PreAuthorize("hasRole('RECEPTIONIST')")
    public ResponseEntity<ConfirmAppointmentResponseDto> confirmAppointment(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID appointmentId
    ){
        UUID receptionistId = JwtUtil.getUserId(jwt);

        Appointment confirmedAppointment = appointmentService.confirmAppointment(receptionistId, appointmentId);
        ConfirmAppointmentResponseDto responseDto = appointmentMapper.toConfirmAppointmentResponseDto(confirmedAppointment);

        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/cancel")
    @PreAuthorize("hasAnyRole('PATIENT', 'RECEPTIONIST')")
    public ResponseEntity<CancelAppointmentResponseDto> cancelAppointment(
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody @Valid CancelAppointmentRequestDto requestDto
    ){
        CancelAppointmentRequest request = appointmentMapper.fromCancelAppointmentRequestDto(requestDto);
        UUID cancellerId = JwtUtil.getUserId(jwt);
        Appointment appointment = appointmentService.cancelAppointment(cancellerId, request);

        CancelAppointmentResponseDto responseDto = appointmentMapper.toCancelAppointmentResponseDto(appointment);
        
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('RECEPTIONIST')")
    public ResponseEntity<Page<ListAppointmentResponseDto>> searchAppointments(
        @AuthenticationPrincipal Jwt jwt,
        Pageable pageable,
        @RequestBody @Valid ListAppointmentRequestDto requestDto
    ){
        ListAppointmentRequest request = appointmentMapper.fromListAppointmentRequestDto(requestDto);
        Page<Appointment> appointments = appointmentService.listAppointment(request, pageable);
        return ResponseEntity.ok(appointments.map(appointmentMapper::toListAppointmentResponseDto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<Page<ListAppointmentResponseDto>> listAppointments(
        @AuthenticationPrincipal Jwt jwt,
        Pageable pageable,
        @RequestBody @Valid ListAppointmentRequestDto requestDto
    ){
        ListAppointmentRequest request = appointmentMapper.fromListAppointmentRequestDto(requestDto);
        UUID requesterId = JwtUtil.getUserId(jwt);
        Page<Appointment> appointments;
        RoleEnum role = JwtUtil.getRole(jwt);

        switch (role){
            case PATIENT -> appointments = appointmentService.listDoctorAppointment(requesterId, request, pageable);
            case DOCTOR -> appointments = appointmentService.listPatientAppointment(requesterId, request, pageable);
            case RECEPTIONIST -> appointments = appointmentService.listConfirmAppointment(requesterId, request, pageable);
            default -> throw new RoleNotFoundException();
        }
         
        return ResponseEntity.ok(appointments.map(appointmentMapper::toListAppointmentResponseDto));
    }

    @PostMapping(path = "/test")
    public ResponseEntity<CreateAppointmentResponseDto> test(
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody @Valid CreateAppointmentRequestDto requestDto
    ){

        return ResponseEntity.ok().build();

    }
}
