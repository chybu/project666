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

        User doctor = requireActiveUserByRole(doctorId, RoleEnum.DOCTOR);

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
        requireActiveUserByRole(patient.getId(), RoleEnum.PATIENT);
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
        requireActiveUserByRole(doctorId, RoleEnum.DOCTOR);

        LabRequest labRequest = labRequestRepository.findByIdAndDoctorId(requestId, doctorId)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("Lab request with ID %s not found", requestId)
            ));

        requireActiveUserByRole(labRequest.getPatient().getId(), RoleEnum.PATIENT);

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
    public LabRequest getLabRequestForDoctor(UUID doctorId, UUID requestId) {
        requireActiveUserByRole(doctorId, RoleEnum.DOCTOR);

        LabRequest labRequest = labRequestRepository.findDetailByIdAndDoctorId(requestId, doctorId)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("Lab request with ID %s not found", requestId)
            ));

        requireActiveUserByRole(labRequest.getPatient().getId(), RoleEnum.PATIENT);
        return labRequest;
    }

    @Override
    public LabRequest getSharedLabRequestForDoctor(UUID doctorId, UUID requestId) {
        requireActiveUserByRole(doctorId, RoleEnum.DOCTOR);

        LabRequest labRequest = labRequestRepository.findDetailById(requestId)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("Lab request with ID %s not found", requestId)
            ));

        requireActiveUserByRole(labRequest.getPatient().getId(), RoleEnum.PATIENT);

        boolean hasApprovedAccess = patientRecordAccessRepository
            .findPatientIdsByDoctorIdAndRecordTypeAndStatus(
                doctorId,
                PatientRecordTypeEnum.LAB_REQUEST,
                PatientRecordAccessStatusEnum.APPROVED
            )
            .contains(labRequest.getPatient().getId());

        if (!hasApprovedAccess || doctorId.equals(labRequest.getDoctor().getId())) {
            throw new NoSuchElementException(String.format("Lab request with ID %s not found", requestId));
        }

        return labRequest;
    }

    @Override
    @Transactional
    public Page<LabTest> listLabTestForLabTechnician(UUID labTechnicianId, ListLabTestRequest request, Pageable pageable) {
        Map<RoleEnum, UserLookup> userLookupMap = new HashMap<>();
        userLookupMap.put(RoleEnum.LAB_TECHNICIAN, new UserLookup(labTechnicianId, RoleEnum.LAB_TECHNICIAN, false));
        userLookupMap.put(RoleEnum.PATIENT, new UserLookup(request.getPatientId(), RoleEnum.PATIENT, true));
        userLookupMap.put(RoleEnum.DOCTOR, new UserLookup(request.getDoctorId(), RoleEnum.DOCTOR, true));
        validateUserLookups(userLookupMap.values());

        // build specification
        Specification<LabTest> spec = LabTestSpecification.alwaysTrue();

        UUID patientId = getLookupId(userLookupMap, RoleEnum.PATIENT);
        if (patientId!=null) spec = spec.and(LabTestSpecification.byPatient(patientId));

        UUID doctorId = getLookupId(userLookupMap, RoleEnum.DOCTOR);
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
    @Transactional
    public Page<PatientLabRequestResponseDto> listLabRequestForPatient(UUID patientId, ListLabRequestRequest request, Pageable pageable) {
        Map<RoleEnum, UserLookup> userLookupMap = new HashMap<>();
        userLookupMap.put(RoleEnum.PATIENT, new UserLookup(patientId, RoleEnum.PATIENT, false));
        userLookupMap.put(RoleEnum.DOCTOR, new UserLookup(request.getDoctorId(), RoleEnum.DOCTOR, true));
        validateUserLookups(userLookupMap.values());
        validateListLabRequest(request);

        Specification<LabRequest> spec = LabRequestSpecification.alwaysTrue();

        if (request.getStatus() != null) spec = spec.and(LabRequestSpecification.byStatus(request.getStatus()));

        if (request.getMinDate() != null || request.getMaxDate() != null) {
            spec = spec.and(LabRequestSpecification.byCreatedAtRange(request.getMinDate(), request.getMaxDate()));
        }

        if (request.getCreatedAtDate()!=null) spec = spec.and(LabRequestSpecification.byCreatedAtDate(request.getCreatedAtDate()));

        spec = spec.and(LabRequestSpecification.byPatient(patientId));

        UUID doctorId = getLookupId(userLookupMap, RoleEnum.DOCTOR);
        if (doctorId!=null) spec = spec.and(LabRequestSpecification.byDoctor(doctorId));

        UUID appointmentId = request.getAppointmentId();
        if (appointmentId!=null) spec = spec.and(LabRequestSpecification.byAppointment(appointmentId));

        // use the dto to hide the lab technician note and doctor note from the patient
       return labRequestRepository.findAll(spec, pageable).map(labRequest -> {
    PatientLabRequestResponseDto dto = labMapper.toPatientLabRequestResponseDto(labRequest);
    if (dto.getLabTests() == null) {
        dto.setLabTests(new java.util.ArrayList<>());
    }
    return dto;
});
    }

    @Override
    public Page<LabRequest> listLabRequestForDoctor(UUID doctorId, ListLabRequestRequest request, Pageable pageable) {
        Map<RoleEnum, UserLookup> userLookupMap = new HashMap<>();
        userLookupMap.put(RoleEnum.DOCTOR, new UserLookup(doctorId, RoleEnum.DOCTOR, false));
        userLookupMap.put(RoleEnum.PATIENT, new UserLookup(request.getPatientId(), RoleEnum.PATIENT, true));
        validateUserLookups(userLookupMap.values());
        validateListLabRequest(request);

        Specification<LabRequest> spec = LabRequestSpecification.alwaysTrue();

        if (request.getStatus() != null) spec = spec.and(LabRequestSpecification.byStatus(request.getStatus()));

        if (request.getMinDate() != null || request.getMaxDate() != null) {
            spec = spec.and(LabRequestSpecification.byCreatedAtRange(request.getMinDate(), request.getMaxDate()));
        }

        if (request.getCreatedAtDate()!=null) spec = spec.and(LabRequestSpecification.byCreatedAtDate(request.getCreatedAtDate()));

        UUID patientId = getLookupId(userLookupMap, RoleEnum.PATIENT);
        if (patientId!=null) spec = spec.and(LabRequestSpecification.byPatient(patientId));

        spec = spec.and(LabRequestSpecification.byDoctor(doctorId));

        UUID appointmentId = request.getAppointmentId();
        if (appointmentId!=null) spec = spec.and(LabRequestSpecification.byAppointment(appointmentId));

        return labRequestRepository.findAll(spec, pageable);
    }

    private void validateListLabRequest(ListLabRequestRequest request) {
        if (
            request.getMinDate() != null
                && request.getMaxDate() != null
                && request.getMinDate().isAfter(request.getMaxDate())
        ) {
            throw new IllegalArgumentException("min date must be on or before max date");
        }
    }

    @Override
    public Page<LabRequest> listLabRequestForNewDoctor(UUID doctorId, ListLabRequestRequest request, Pageable pageable) {
        Map<RoleEnum, UserLookup> userLookupMap = new HashMap<>();
        userLookupMap.put(RoleEnum.DOCTOR, new UserLookup(doctorId, RoleEnum.DOCTOR, false));
        userLookupMap.put(RoleEnum.PATIENT, new UserLookup(request.getPatientId(), RoleEnum.PATIENT, true));
        validateUserLookups(userLookupMap.values());

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

        UUID patientId = getLookupId(userLookupMap, RoleEnum.PATIENT);
        if (patientId != null) {
            spec = spec.and(LabRequestSpecification.byPatient(patientId));
        }

        UUID appointmentId = request.getAppointmentId();
        if (appointmentId != null) {
            spec = spec.and(LabRequestSpecification.byAppointment(appointmentId));
        }

        return labRequestRepository.findAll(spec, pageable);
    }


    @Override
    @Transactional
    public Page<LabRequest> listLabRequestForLabTechnician(UUID labTechnicianId, ListLabRequestRequest request, Pageable pageable) {
        Map<RoleEnum, UserLookup> userLookupMap = new HashMap<>();
        userLookupMap.put(RoleEnum.LAB_TECHNICIAN, new UserLookup(labTechnicianId, RoleEnum.LAB_TECHNICIAN, false));
        userLookupMap.put(RoleEnum.PATIENT, new UserLookup(request.getPatientId(), RoleEnum.PATIENT, true));
        userLookupMap.put(RoleEnum.DOCTOR, new UserLookup(request.getDoctorId(), RoleEnum.DOCTOR, true));
        validateUserLookups(userLookupMap.values());
        
        Specification<LabRequest> spec = LabRequestSpecification.alwaysTrue();

        spec = spec.and(LabRequestSpecification.unfinishedOnly());

        if (request.getCreatedAtDate()!=null) spec = spec.and(LabRequestSpecification.byCreatedAtDate(request.getCreatedAtDate()));

        UUID patientId = getLookupId(userLookupMap, RoleEnum.PATIENT);
        if (patientId!=null) spec = spec.and(LabRequestSpecification.byPatient(patientId));

        UUID doctorId = getLookupId(userLookupMap, RoleEnum.DOCTOR);
        if (doctorId!=null) spec = spec.and(LabRequestSpecification.byDoctor(doctorId));

        return labRequestRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional
    public LabTest claimLabTest(UUID labTechnicianId, UUID labTestId) {
        User labTechnician = requireActiveUserByRole(labTechnicianId, RoleEnum.LAB_TECHNICIAN);

        LabTest labTest = labTestRepository.findById(labTestId)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("Lab test with ID %s not found", labTestId)
            ));

        requireActiveUserByRole(labTest.getPatient().getId(), RoleEnum.PATIENT);

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
        requireActiveUserByRole(labTechnicianId, RoleEnum.LAB_TECHNICIAN);
        
        LabTest labTest = labTestRepository.findByIdAndLabTechnicianId(request.getLabTestId(), labTechnicianId)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("Lab test with ID %s not found", request.getLabTestId())
            ));

        requireActiveUserByRole(labTest.getPatient().getId(), RoleEnum.PATIENT);

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
        requireActiveUserByRole(labTechnicianId, RoleEnum.LAB_TECHNICIAN);

        LabTest labTest = labTestRepository.findByIdAndLabTechnicianId(labTestId, labTechnicianId)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("Lab test with ID %s not found", labTestId)
            ));

        requireActiveUserByRole(labTest.getPatient().getId(), RoleEnum.PATIENT);

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

    private User requireActiveUserByRole(UUID userId, RoleEnum role) {
        return userRepository.findByIdAndRoleAndDeletedFalse(userId, role)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("%s with ID %s not found", role.name(), userId)
            ));
    }

    private void validateUserLookups(Iterable<UserLookup> userLookups) {
        for (UserLookup userLookup : userLookups) {
            if (userLookup == null || userLookup.id() == null) {
                continue;
            }

            boolean exists = userLookup.filter()
                ? userRepository.findByIdAndRole(userLookup.id(), userLookup.role()).isPresent()
                : userRepository.findByIdAndRoleAndDeletedFalse(userLookup.id(), userLookup.role()).isPresent();

            if (!exists) {
                throw new NoSuchElementException(
                    String.format("%s with ID %s not found", userLookup.role().name(), userLookup.id())
                );
            }
        }
    }

    private UUID getLookupId(Map<RoleEnum, UserLookup> userLookupMap, RoleEnum role) {
        UserLookup lookup = userLookupMap.get(role);
        return lookup != null ? lookup.id() : null;
    }

    private record UserLookup(UUID id, RoleEnum role, boolean filter) {
    }
}
