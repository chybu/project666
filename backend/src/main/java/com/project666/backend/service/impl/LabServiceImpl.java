package com.project666.backend.service.impl;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.project666.backend.domain.CreateLabRequestRequest;
import com.project666.backend.domain.CreateLabRequestRequest.LabTestRequest;
import com.project666.backend.domain.dto.PatientLabRequestResponseDto;
import com.project666.backend.domain.ListLabRequestRequest;
import com.project666.backend.domain.ListLabTestRequest;
import com.project666.backend.domain.UpdateLabTestRequest;
import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.entity.AppointmentStatusEnum;
import com.project666.backend.domain.entity.LabRequest;
import com.project666.backend.domain.entity.LabRequestStatusEnum;
import com.project666.backend.domain.entity.LabTest;
import com.project666.backend.domain.entity.LabTestStatusEnum;
import com.project666.backend.domain.entity.PatientRecordAccessStatusEnum;
import com.project666.backend.domain.entity.PatientRecordTypeEnum;
import com.project666.backend.domain.entity.RoleEnum;
import com.project666.backend.domain.entity.User;
import com.project666.backend.mapper.LabMapper;
import com.project666.backend.repository.AppointmentRepository;
import com.project666.backend.repository.LabRequestRepository;
import com.project666.backend.repository.LabTestRepository;
import com.project666.backend.repository.PatientRecordAccessRepository;
import com.project666.backend.repository.UserRepository;
import com.project666.backend.service.BillService;
import com.project666.backend.service.LabService;
import com.project666.backend.specification.LabRequestSpecification;
import com.project666.backend.specification.LabTestSpecification;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LabServiceImpl implements LabService{
    
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final LabRequestRepository labRequestRepository;
    private final LabTestRepository labTestRepository;
    private final PatientRecordAccessRepository patientRecordAccessRepository;
    private final LabMapper labMapper;
    private final BillService billService;

    @Override
    @Transactional
    public LabRequest createLabRequest(UUID doctorId, CreateLabRequestRequest request) {
        validateCreateLabRequest(request);

        User doctor = userRepository.findByIdAndRole(doctorId, RoleEnum.DOCTOR)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("DOCTOR with ID %s not found", doctorId)
            )
        );

        Appointment appointment = appointmentRepository.findByIdAndDoctorId(request.getAppointmentId(), doctorId)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("Appointment with ID %s not found", request.getAppointmentId())
            )
        );

        // only when the receptionist confirms the patient going to the appointment then the doctor can make the lab request for that appointment
        if (!appointment.getStatus().equals(AppointmentStatusEnum.COMPLETED)){
            throw new IllegalArgumentException("Cannot make lab request with unconfirmed appointment");
        }

        User patient = appointment.getPatient();
        checkDuplicateLabRequest(patient.getId(), request.getLabTests());

        LabRequest labRequest = new LabRequest();
        labRequest.setDoctor(doctor);
        labRequest.setPatient(patient);
        labRequest.setAppointment(appointment);
        labRequest.setStatus(LabRequestStatusEnum.REQUESTED);

        for (LabTestRequest labTestRequest : request.getLabTests()) {
            LabTest test = new LabTest();
            test.setCode(labTestRequest.getCode());
            test.setName(labTestRequest.getName());
            test.setUnit(labTestRequest.getUnit());
            test.setDoctorNote(labTestRequest.getDoctorNote());
            test.setStatus(LabTestStatusEnum.REQUESTED);
            test.setPatient(patient);
            
            // No need for a LabTestRepository here because LabTest will be persisted through LabRequest cascade when the parent is saved.
            labRequest.addLabTest(test);
        }

        return labRequestRepository.save(labRequest);
    }

    @Override
    @Transactional
    public LabRequest cancelLabRequest(UUID doctorId, UUID requestId) {
        userRepository.findByIdAndRole(doctorId, RoleEnum.DOCTOR)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("Doctor with ID %s not found", doctorId)
            ));

        LabRequest labRequest = labRequestRepository.findByIdAndDoctorId(requestId, doctorId)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("Lab request with ID %s not found", requestId)
            ));

        if (labRequest.getStatus() == LabRequestStatusEnum.COMPLETED) {
            throw new IllegalArgumentException("Cannot cancel a completed lab request");
        }

        if (labRequest.getStatus() == LabRequestStatusEnum.CANCELLED) {
            throw new IllegalArgumentException("Lab request is already cancelled");
        }

        labRequest.setStatus(LabRequestStatusEnum.CANCELLED);

        for (LabTest labTest : labRequest.getLabTests()) {
            labTest.setStatus(LabTestStatusEnum.CANCELLED);
        }

        return labRequestRepository.save(labRequest);
    }

    @Override
    public Page<LabTest> listLabTestForLabTechnician(UUID labTechnicianId, ListLabTestRequest request, Pageable pageable) {
        Map<RoleEnum, UUID> roleMap = new HashMap<>();
        roleMap.put(RoleEnum.LAB_TECHNICIAN, labTechnicianId);
        roleMap.put(RoleEnum.PATIENT, request.getPatientId());
        roleMap.put(RoleEnum.DOCTOR, request.getDoctorId());
        checkForRoleExistance(roleMap);

        // build specification
        Specification<LabTest> spec = LabTestSpecification.alwaysTrue();

        UUID patientId = roleMap.get(RoleEnum.PATIENT);
        if (patientId!=null) spec = spec.and(LabTestSpecification.byPatient(patientId));

        UUID doctorId = roleMap.get(RoleEnum.DOCTOR);
        if (doctorId!=null) spec = spec.and(LabTestSpecification.byDoctor(doctorId));

        spec = spec.and(LabTestSpecification.byLabTechnician(labTechnicianId));

        LabTestStatusEnum status = request.getStatus();
        if (status != null) spec = spec.and(LabTestSpecification.byStatus(status));

        String code = request.getCode();
        if (code!=null && !code.isBlank()) spec = spec.and(LabTestSpecification.byCode(request.getCode().trim()));

        String name = request.getName();
        if (name!=null && !name.isBlank()) spec = spec.and(LabTestSpecification.byName(request.getName().trim()));

        String unit = request.getUnit();
        if (unit!=null && !unit.isBlank()) spec = spec.and(LabTestSpecification.byUnit(request.getUnit().trim()));

        LocalDate createdAtDate = request.getCreatedAtDate();
        if (createdAtDate!=null) spec = spec.and(LabTestSpecification.byCreatedAtDate(createdAtDate));

        return labTestRepository.findAll(spec, pageable);
    }

    @Override
    public Page<PatientLabRequestResponseDto> listLabRequestForPatient(UUID patientId, ListLabRequestRequest request, Pageable pageable) {
        Map<RoleEnum, UUID> roleMap = new HashMap<>();
        roleMap.put(RoleEnum.PATIENT, patientId);
        roleMap.put(RoleEnum.DOCTOR, request.getDoctorId());
        checkForRoleExistance(roleMap);

        Specification<LabRequest> spec = LabRequestSpecification.alwaysTrue();

        if (request.getStatus() != null) spec = spec.and(LabRequestSpecification.byStatus(request.getStatus()));

        if (request.getCreatedAtDate()!=null) spec = spec.and(LabRequestSpecification.byCreatedAtDate(request.getCreatedAtDate()));

        spec = spec.and(LabRequestSpecification.byPatient(patientId));

        UUID doctorId = roleMap.get(RoleEnum.DOCTOR);
        if (doctorId!=null) spec = spec.and(LabRequestSpecification.byDoctor(doctorId));

        UUID appointmentId = request.getAppointmentId();
        if (appointmentId!=null) spec = spec.and(LabRequestSpecification.byAppointment(appointmentId));

        // use the dto to hide the lab technician note and doctor note from the patient
        return labRequestRepository.findAll(spec, pageable).map(labMapper::toPatientLabRequestResponseDto);
    }

    @Override
    public Page<LabRequest> listLabRequestForDoctor(UUID doctorId, ListLabRequestRequest request, Pageable pageable) {
        Map<RoleEnum, UUID> roleMap = new HashMap<>();
        roleMap.put(RoleEnum.PATIENT, request.getPatientId());
        roleMap.put(RoleEnum.DOCTOR, doctorId);
        checkForRoleExistance(roleMap);

        Specification<LabRequest> spec = LabRequestSpecification.alwaysTrue();

        if (request.getStatus() != null) spec = spec.and(LabRequestSpecification.byStatus(request.getStatus()));

        if (request.getCreatedAtDate()!=null) spec = spec.and(LabRequestSpecification.byCreatedAtDate(request.getCreatedAtDate()));

        UUID patientId = roleMap.get(RoleEnum.PATIENT);
        if (patientId!=null) spec = spec.and(LabRequestSpecification.byPatient(patientId));

        spec = spec.and(LabRequestSpecification.byDoctor(doctorId));

        UUID appointmentId = request.getAppointmentId();
        if (appointmentId!=null) spec = spec.and(LabRequestSpecification.byAppointment(appointmentId));

        return labRequestRepository.findAll(spec, pageable);
    }

    @Override
    public Page<LabRequest> listLabRequestForNewDoctor(UUID doctorId, ListLabRequestRequest request, Pageable pageable) {
        userRepository.findByIdAndRole(doctorId, RoleEnum.DOCTOR)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("DOCTOR with ID %s not found", doctorId)
            ));

        List<UUID> approvedPatientIds = patientRecordAccessRepository
            .findPatientIdsByDoctorIdAndRecordTypeAndStatus(
                doctorId,
                PatientRecordTypeEnum.LAB_REQUEST,
                PatientRecordAccessStatusEnum.APPROVED
            );

        if (approvedPatientIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Specification<LabRequest> spec = LabRequestSpecification.alwaysTrue();

        // only lab requests of patients who granted this doctor access
        spec = spec.and((root, query, cb) ->
            root.get("patient").get("id").in(approvedPatientIds)
        );

        // exclude lab requests created by this same doctor
        spec = spec.and((root, query, cb) ->
            cb.notEqual(root.get("doctor").get("id"), doctorId)
        );

        if (request.getStatus() != null) {
            spec = spec.and(LabRequestSpecification.byStatus(request.getStatus()));
        }

        if (request.getCreatedAtDate() != null) {
            spec = spec.and(LabRequestSpecification.byCreatedAtDate(request.getCreatedAtDate()));
        }

        UUID patientId = request.getPatientId();
        if (patientId != null) {
            userRepository.findByIdAndRole(patientId, RoleEnum.PATIENT)
                .orElseThrow(() -> new NoSuchElementException(
                    String.format("PATIENT with ID %s not found", patientId)
                ));

            spec = spec.and(LabRequestSpecification.byPatient(patientId));
        }

        UUID appointmentId = request.getAppointmentId();
        if (appointmentId != null) {
            spec = spec.and(LabRequestSpecification.byAppointment(appointmentId));
        }

        return labRequestRepository.findAll(spec, pageable);
    }


    @Override
    public Page<LabRequest> listLabRequestForLabTechnician(UUID labTechnicianId, ListLabRequestRequest request, Pageable pageable) {
        
        Map<RoleEnum, UUID> roleMap = new HashMap<>();
        roleMap.put(RoleEnum.LAB_TECHNICIAN, labTechnicianId);
        roleMap.put(RoleEnum.PATIENT, request.getPatientId());
        roleMap.put(RoleEnum.DOCTOR, request.getDoctorId());
        checkForRoleExistance(roleMap);
        
        Specification<LabRequest> spec = LabRequestSpecification.alwaysTrue();

        spec = spec.and(LabRequestSpecification.unfinishedOnly());

        if (request.getCreatedAtDate()!=null) spec = spec.and(LabRequestSpecification.byCreatedAtDate(request.getCreatedAtDate()));

        UUID patientId = roleMap.get(RoleEnum.PATIENT);
        if (patientId!=null) spec = spec.and(LabRequestSpecification.byPatient(patientId));

        UUID doctorId = roleMap.get(RoleEnum.DOCTOR);
        if (doctorId!=null) spec = spec.and(LabRequestSpecification.byDoctor(doctorId));

        return labRequestRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional
    public LabTest claimLabTest(UUID labTechnicianId, UUID labTestId) {
        User labTechnician = userRepository.findByIdAndRole(labTechnicianId, RoleEnum.LAB_TECHNICIAN)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("Lab technician with ID %s not found", labTechnicianId)
            ));

        LabTest labTest = labTestRepository.findById(labTestId)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("Lab test with ID %s not found", labTestId)
            ));

        if (labTest.getStatus() != LabTestStatusEnum.REQUESTED) {
            throw new IllegalArgumentException("Only requested lab tests can be claimed");
        }

        LabRequest labRequest = labTest.getLabRequest();
        if (LabRequestStatusEnum.REQUESTED.equals(labRequest.getStatus())) labRequest.setStatus(LabRequestStatusEnum.IN_PROGRESS);

        labTest.setLabTechnician(labTechnician);
        labTest.setStatus(LabTestStatusEnum.IN_PROGRESS);

        return labTestRepository.save(labTest);
    }

    @Override
    @Transactional
    public LabTest updateLabTest(UUID labTechnicianId, UpdateLabTestRequest request) {
        userRepository.findByIdAndRole(labTechnicianId, RoleEnum.LAB_TECHNICIAN)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("Lab technician with ID %s not found", labTechnicianId)
            ));
        
        LabTest labTest = labTestRepository.findByIdAndLabTechnicianId(request.getLabTestId(), labTechnicianId)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("Lab test with ID %s not found", request.getLabTestId())
            ));

        if (labTest.getStatus() != LabTestStatusEnum.IN_PROGRESS) {
            throw new IllegalArgumentException("Only in-progress lab tests can be updated");
        }

        String code = request.getCode();
        if (code!=null && !code.isBlank()) {
            labTest.setCode(request.getCode().trim());
        }
        String name = request.getName();
        if (name != null && !name.isBlank()) {
            labTest.setName(name.trim());
        }
        String unit = request.getUnit();
        if (unit != null && !unit.isBlank()) {
            labTest.setUnit(unit.trim());
        }
        String result = request.getResult();
        if (result != null && !result.isBlank()) {
            labTest.setResult(result.trim());
        }
        String labTechnicianNote = request.getLabTechnicianNote();
        if (labTechnicianNote != null && !labTechnicianNote.isBlank()) {
            labTest.setLabTechnicianNote(labTechnicianNote.trim());
        }

        return labTestRepository.save(labTest);
    }

    @Override
    @Transactional
    public LabTest submitLabTest(UUID labTechnicianId, UUID labTestId) {
        userRepository.findByIdAndRole(labTechnicianId, RoleEnum.LAB_TECHNICIAN)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("Lab technician with ID %s not found", labTechnicianId)
            ));

        LabTest labTest = labTestRepository.findByIdAndLabTechnicianId(labTestId, labTechnicianId)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("Lab test with ID %s not found", labTestId)
            ));

        if (labTest.getStatus() != LabTestStatusEnum.IN_PROGRESS) {
            throw new IllegalArgumentException("Only in-progress lab tests can be submitted");
        }

        if (labTest.getResult() == null || labTest.getResult().isBlank()) {
            throw new IllegalArgumentException("Cannot submit lab test without result");
        }

        labTest.setStatus(LabTestStatusEnum.COMPLETED);

        // add lab bill
        billService.generateBillForLabTest(labTest);

        // finish lab request if this is the last test
        LabRequest labRequest = labTest.getLabRequest();
        if (checkLabRequestComplete(labRequest)) labRequest.setStatus(LabRequestStatusEnum.COMPLETED);

        return labTestRepository.save(labTest);
    }

    private boolean checkLabRequestComplete(LabRequest labRequest){
        for (LabTest labTest : labRequest.getLabTests()){
            if (!labTest.getStatus().equals(LabTestStatusEnum.COMPLETED)) return false;
        }
        return true;
    }

    private void checkForRoleExistance(Map<RoleEnum, UUID> roleMap){
        roleMap.forEach((role, id) -> {
            if (id!=null){
                userRepository.findByIdAndRole(id, role).orElseThrow(() -> new NoSuchElementException(
                    String.format("%s with ID %s not found", role.name(), id)
                ));
            }
        });
    }

    private void checkDuplicateLabRequest(UUID patientId, List<CreateLabRequestRequest.LabTestRequest> labTests) {
        Set<String> normalizedNames = new HashSet<>();

        for (CreateLabRequestRequest.LabTestRequest labTestRequest : labTests) {
            String name = labTestRequest.getName();

            if (name == null || name.isBlank()) {
                continue;
            }

            String normalized = name.trim().toLowerCase();

            // duplicate inside the same request
            if (!normalizedNames.add(normalized)) {
                throw new IllegalArgumentException("Duplicated lab test inside the same request: " + name);
            }

            // duplicate against active tests in DB
            boolean exists = labTestRepository.existsByLabRequestPatientIdAndNameIgnoreCaseAndStatusIn(
                patientId,
                name.trim(),
                List.of(
                    LabTestStatusEnum.REQUESTED,
                    LabTestStatusEnum.IN_PROGRESS
                )
            );

            if (exists) {
                throw new IllegalArgumentException("Duplicated active lab test: " + name);
            }
        }
    }

    private void validateCreateLabRequest(CreateLabRequestRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Create lab request is required");
        }

        if (request.getAppointmentId() == null) {
            throw new IllegalArgumentException("Lab request appointmentId is required");
        }

        if (request.getLabTests() == null || request.getLabTests().isEmpty()) {
            throw new IllegalArgumentException("Lab request must contain at least one lab test");
        }

        for (CreateLabRequestRequest.LabTestRequest labTestRequest : request.getLabTests()) {
            if (labTestRequest == null || labTestRequest.getName() == null || labTestRequest.getName().isBlank()) {
                throw new IllegalArgumentException("Lab test name is required");
            }
        }
    }
}
