package demo.controller.api;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.domain.CancelAppointmentRequest;
import demo.domain.CreateAppointmentRequest;
import demo.domain.ListAppointmentRequest;
import demo.domain.dtos.CancelAppointmentRequestDto;
import demo.domain.dtos.CancelAppointmentResponseDto;
import demo.domain.dtos.ConfirmAppointmentResponseDto;
import demo.domain.dtos.CreateAppointmentRequestDto;
import demo.domain.dtos.CreateAppointmentResponseDto;
import demo.domain.dtos.ListAppointmentRequestDto;
import demo.domain.dtos.ListAppointmentResponseDto;
import demo.domain.entities.Appointment;
import demo.domain.entities.RoleEnum;
import demo.exception.RoleNotFoundException;
import demo.mapper.AppointmentMapper;
import demo.service.AppointmentService;
import demo.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentMapper appointmentMapper;
    private final AppointmentService appointmentService;

    @PutMapping("/{appointmentId}/confirm")
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

    @PutMapping("/cancel")
    @PreAuthorize("hasAnyRole('PATIENT', 'RECEPTIONIST')")
    public ResponseEntity<CancelAppointmentResponseDto> cancelAppointment(
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody @Valid CancelAppointmentRequestDto requestDto
    ){
        CancelAppointmentRequest request = appointmentMapper.fromCancelAppointmentRequestDto(requestDto);
        UUID cancellerId = JwtUtil.getUserId(jwt);
        RoleEnum cancellerRole = JwtUtil.getRole(jwt);
        Appointment appointment = appointmentService.cancelAppointment(cancellerId, cancellerRole, request);

        CancelAppointmentResponseDto responseDto = appointmentMapper.toCancelAppointmentResponseDto(appointment);
        
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/create")
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

    @PostMapping("/search")
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

    @PostMapping("/list")
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
