# Nurse Feature Guide: Precheck

## 1. What this feature does

The nurse feature is for managing a new patient record called `Precheck`.

Business flow:

1. Patient books an appointment.
2. Receptionist checks the patient in.
3. In this project, that check-in is currently represented by appointment status `COMPLETED`.
4. Before the doctor sees the patient, the nurse records vital signs in a `Precheck`.
5. The doctor uses that precheck during the consultation.

Typical data inside precheck:

- pulse
- sugar
- temperature
- height
- weight
- note

Important business rules:

- A nurse can create a precheck only for an attended appointment.
- In this codebase, attended means the appointment has status `COMPLETED`.
- Only one valid precheck is allowed for each appointment.
- If the nurse makes a mistake, they cannot edit the old precheck into history.
- They must cancel the wrong precheck and create a new one.
- Cancellation is allowed only during the appointment time range:
  `appointment.startTime <= now <= appointment.endTime`
- Patient, appointment doctor, and the nurse who created the precheck can view it directly.
- Another doctor cannot view it directly.
- Another doctor must request patient record access, same idea as prescription/lab shared access.

## 2. Main design idea

Create a new entity called `Precheck`.

Do not store precheck fields inside `Appointment`.

Why:

- one appointment can have precheck history
- cancelled prechecks should still be kept for audit/history
- business rule says only one valid precheck, not only one total precheck

Recommended status enum:

```java
public enum PrecheckStatusEnum {
    VALID,
    CANCELLED
}
```

Recommended visibility model:

- patient = from appointment
- doctor = from appointment
- nurse = the nurse who created the precheck

Important note:

`Appointment` currently has `doctor` and `patient`, but no `nurse`.
So for now, "nurse belongs to the appointment" should mean:
the nurse who created that appointment's precheck.

That is the cleanest design for the current codebase.

## 3. Files your junior should create

### Entity / enum

- `backend/src/main/java/com/project666/backend/domain/entity/Precheck.java`
- `backend/src/main/java/com/project666/backend/domain/entity/PrecheckStatusEnum.java`

### Request objects

- `backend/src/main/java/com/project666/backend/domain/CreatePrecheckRequest.java`
- `backend/src/main/java/com/project666/backend/domain/ListPrecheckRequest.java`

### DTOs

- `backend/src/main/java/com/project666/backend/domain/dto/CreatePrecheckRequestDto.java`
- `backend/src/main/java/com/project666/backend/domain/dto/PrecheckResponseDto.java`
- `backend/src/main/java/com/project666/backend/domain/dto/ListPrecheckRequestDto.java`

### Mapper

- `backend/src/main/java/com/project666/backend/mapper/PrecheckMapper.java`

### Repository

- `backend/src/main/java/com/project666/backend/repository/PrecheckRepository.java`

### Specification

- `backend/src/main/java/com/project666/backend/specification/PrecheckSpecification.java`

### Service

- `backend/src/main/java/com/project666/backend/service/PrecheckService.java`
- `backend/src/main/java/com/project666/backend/service/impl/PrecheckServiceImpl.java`

### Controller

- `backend/src/main/java/com/project666/backend/controller/PrecheckController.java`

### Existing files to update

- `backend/src/main/java/com/project666/backend/domain/entity/User.java`
- `backend/src/main/java/com/project666/backend/domain/entity/Appointment.java`
- `backend/src/main/java/com/project666/backend/domain/entity/PatientRecordTypeEnum.java`

Optional but recommended:

- database migration / SQL index for one valid precheck per appointment

## 4. How the entity should look

Use `LabRequest`, `PatientRecordAccess`, and `AppointmentBill` style as reference.

Suggested entity:

```java
@Entity
@Table(name = "prechecks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Precheck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, updatable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false, updatable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false, updatable = false)
    private User doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nurse_id", nullable = false, updatable = false)
    private User nurse;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PrecheckStatusEnum status;

    @Column(name = "pulse", nullable = false)
    private Integer pulse;

    @Column(name = "sugar", nullable = false)
    private Double sugar;

    @Column(name = "temperature", nullable = false)
    private Double temperature;

    @Column(name = "height", nullable = false)
    private Double height;

    @Column(name = "weight", nullable = false)
    private Double weight;

    @Column(name = "note")
    private String note;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
}
```

### Why store patient and doctor again if appointment already has them?

Because your project already likes direct relations for querying and permissions.
It makes repository methods and specifications simpler.

When creating the precheck:

- `patient = appointment.getPatient()`
- `doctor = appointment.getDoctor()`
- `nurse = current nurse`

## 5. Update existing entities

### `PatientRecordTypeEnum`

Add one more type:

```java
PRECHECK
```

This is needed so another doctor can request access to view prechecks.

### `User`

Add relation lists similar to the existing project style:

```java
@OneToMany(mappedBy = "nurse", cascade = CascadeType.ALL)
private List<Precheck> nursePrechecks = new ArrayList<>();

@OneToMany(mappedBy = "patient", cascade = CascadeType.ALL)
private List<Precheck> patientPrechecks = new ArrayList<>();

@OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL)
private List<Precheck> doctorPrechecks = new ArrayList<>();
```

### `Appointment`

Add relation:

```java
@OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL)
private List<Precheck> prechecks = new ArrayList<>();
```

## 6. DTOs and request objects

Follow the same split already used in this project:

- DTO = API layer
- request object = service layer

### `CreatePrecheckRequestDto`

Purpose:
what nurse sends to create a precheck

Suggested fields:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePrecheckRequestDto {
    @NotNull
    private UUID appointmentId;

    @NotNull
    @Positive
    private Integer pulse;

    @NotNull
    @Positive
    private Double sugar;

    @NotNull
    @Positive
    private Double temperature;

    @NotNull
    @Positive
    private Double height;

    @NotNull
    @Positive
    private Double weight;

    private String note;
}
```

### `CreatePrecheckRequest`

Same fields, but no validation annotations needed:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePrecheckRequest {
    private UUID appointmentId;
    private Integer pulse;
    private Double sugar;
    private Double temperature;
    private Double height;
    private Double weight;
    private String note;
}
```

### `ListPrecheckRequestDto`

Purpose:
filter list result

Suggested fields:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListPrecheckRequestDto {
    private UUID appointmentId;
    private UUID patientId;
    private UUID doctorId;
    private UUID nurseId;
    private PrecheckStatusEnum status;
    private LocalDate createdAtDate;
}
```

### `ListPrecheckRequest`

Same fields:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListPrecheckRequest {
    private UUID appointmentId;
    private UUID patientId;
    private UUID doctorId;
    private UUID nurseId;
    private PrecheckStatusEnum status;
    private LocalDate createdAtDate;
}
```

### `PrecheckResponseDto`

Use one response DTO for create/list/cancel response.

Suggested fields:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrecheckResponseDto {
    private UUID id;
    private UUID appointmentId;
    private LocalDateTime appointmentStartTime;
    private LocalDateTime appointmentEndTime;
    private String patientFullName;
    private String doctorFullName;
    private String nurseFullName;
    private PrecheckStatusEnum status;
    private Integer pulse;
    private Double sugar;
    private Double temperature;
    private Double height;
    private Double weight;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
}
```

## 7. Mapper

Create `PrecheckMapper` and follow `LabMapper` style.

Suggested mapper:

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PrecheckMapper {

    CreatePrecheckRequest fromCreatePrecheckRequestDto(CreatePrecheckRequestDto dto);

    ListPrecheckRequest fromListPrecheckRequestDto(ListPrecheckRequestDto dto);

    @Mapping(target = "appointmentId", source = "appointment.id")
    @Mapping(target = "appointmentStartTime", source = "appointment.startTime")
    @Mapping(target = "appointmentEndTime", source = "appointment.endTime")
    @Mapping(target = "patientFullName", source = "patient.fullName")
    @Mapping(target = "doctorFullName", source = "doctor.fullName")
    @Mapping(target = "nurseFullName", source = "nurse.fullName")
    PrecheckResponseDto toPrecheckResponseDto(Precheck precheck);
}
```

## 8. Repository

Repository should support:

- create and normal CRUD
- specification listing
- permission lookup methods
- duplicate valid precheck checking

Suggested repository:

```java
@Repository
public interface PrecheckRepository extends
    JpaRepository<Precheck, UUID>,
    JpaSpecificationExecutor<Precheck> {

    Optional<Precheck> findByIdAndNurseId(UUID precheckId, UUID nurseId);

    Optional<Precheck> findByIdAndPatientId(UUID precheckId, UUID patientId);

    Optional<Precheck> findByIdAndDoctorId(UUID precheckId, UUID doctorId);

    boolean existsByAppointmentIdAndStatus(UUID appointmentId, PrecheckStatusEnum status);
}
```

Optional extra query if needed later:

```java
List<Precheck> findByAppointmentIdOrderByCreatedAtDesc(UUID appointmentId);
```

## 9. Specification

Create `PrecheckSpecification` like your other specifications.

Suggested methods:

```java
public final class PrecheckSpecification {

    private PrecheckSpecification() {
    }

    public static Specification<Precheck> alwaysTrue() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<Precheck> byAppointment(UUID appointmentId) {
        return (root, query, cb) ->
            appointmentId == null ? null :
            cb.equal(root.get("appointment").get("id"), appointmentId);
    }

    public static Specification<Precheck> byPatient(UUID patientId) {
        return (root, query, cb) ->
            patientId == null ? null :
            cb.equal(root.get("patient").get("id"), patientId);
    }

    public static Specification<Precheck> byDoctor(UUID doctorId) {
        return (root, query, cb) ->
            doctorId == null ? null :
            cb.equal(root.get("doctor").get("id"), doctorId);
    }

    public static Specification<Precheck> byNurse(UUID nurseId) {
        return (root, query, cb) ->
            nurseId == null ? null :
            cb.equal(root.get("nurse").get("id"), nurseId);
    }

    public static Specification<Precheck> byStatus(PrecheckStatusEnum status) {
        return (root, query, cb) ->
            status == null ? null :
            cb.equal(root.get("status"), status);
    }

    public static Specification<Precheck> byCreatedAtDate(LocalDate createdAtDate) {
        return (root, query, cb) -> {
            if (createdAtDate == null) return null;

            LocalDateTime start = createdAtDate.atStartOfDay();
            LocalDateTime end = createdAtDate.plusDays(1).atStartOfDay();

            return cb.and(
                cb.greaterThanOrEqualTo(root.get("createdAt"), start),
                cb.lessThan(root.get("createdAt"), end)
            );
        };
    }
}
```

## 10. Service interface

Keep the service API simple and consistent with the rest of the codebase.

Suggested interface:

```java
public interface PrecheckService {

    Precheck createPrecheck(UUID nurseId, CreatePrecheckRequest request);

    Precheck cancelPrecheck(UUID nurseId, UUID precheckId);

    Page<Precheck> listPrecheckForPatient(UUID patientId, ListPrecheckRequest request, Pageable pageable);

    Page<Precheck> listPrecheckForDoctor(UUID doctorId, ListPrecheckRequest request, Pageable pageable);

    Page<Precheck> listPrecheckForNurse(UUID nurseId, ListPrecheckRequest request, Pageable pageable);

    Page<Precheck> listSharedPrecheckForDoctor(UUID doctorId, ListPrecheckRequest request, Pageable pageable);
}
```

## 11. Service implementation

This is the most important part.

### Dependencies

Your service implementation will probably need:

```java
private final UserRepository userRepository;
private final AppointmentRepository appointmentRepository;
private final PrecheckRepository precheckRepository;
private final PatientRecordAccessRepository patientRecordAccessRepository;
```

### 11.1 Create precheck

Rules to enforce:

- current user must be a valid nurse
- request must be validated with `validateCreateRequest(request);`
- appointment must exist
- appointment must already be attended (`AppointmentStatusEnum.COMPLETED`)
- only one valid precheck per appointment
- precheck uses patient and doctor from appointment

Very important:
you asked to include this line, so the junior must call it in create:

```java
validateCreateRequest(request);
```

Recommended flow:

```java
@Override
@Transactional
public Precheck createPrecheck(UUID nurseId, CreatePrecheckRequest request) {
    User nurse = userRepository.findByIdAndRole(nurseId, RoleEnum.NURSE)
        .orElseThrow(() -> new NoSuchElementException(
            String.format("NURSE with ID %s not found", nurseId)
        ));

    validateCreateRequest(request);

    Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
        .orElseThrow(() -> new NoSuchElementException(
            String.format("Appointment with ID %s not found", request.getAppointmentId())
        ));

    if (!AppointmentStatusEnum.COMPLETED.equals(appointment.getStatus())) {
        throw new IllegalArgumentException("Cannot create precheck for unattended appointment");
    }

    if (precheckRepository.existsByAppointmentIdAndStatus(
        appointment.getId(),
        PrecheckStatusEnum.VALID
    )) {
        throw new IllegalArgumentException("Appointment already has a valid precheck");
    }

    Precheck precheck = new Precheck();
    precheck.setAppointment(appointment);
    precheck.setPatient(appointment.getPatient());
    precheck.setDoctor(appointment.getDoctor());
    precheck.setNurse(nurse);
    precheck.setStatus(PrecheckStatusEnum.VALID);
    precheck.setPulse(request.getPulse());
    precheck.setSugar(request.getSugar());
    precheck.setTemperature(request.getTemperature());
    precheck.setHeight(request.getHeight());
    precheck.setWeight(request.getWeight());
    precheck.setNote(trimToNull(request.getNote()));

    return precheckRepository.save(precheck);
}
```

### 11.2 `validateCreateRequest(request)`

Since you specifically want it, create a helper method.

Purpose:

- keep `createPrecheck` clean
- centralize business validation

Suggested version:

```java
private void validateCreateRequest(CreatePrecheckRequest request) {
    if (request == null) {
        throw new IllegalArgumentException("Request must not be null");
    }

    if (request.getAppointmentId() == null) {
        throw new IllegalArgumentException("Appointment ID is required");
    }

    if (request.getPulse() == null || request.getPulse() <= 0) {
        throw new IllegalArgumentException("Pulse must be greater than 0");
    }

    if (request.getSugar() == null || request.getSugar() <= 0) {
        throw new IllegalArgumentException("Sugar must be greater than 0");
    }

    if (request.getTemperature() == null || request.getTemperature() <= 0) {
        throw new IllegalArgumentException("Temperature must be greater than 0");
    }

    if (request.getHeight() == null || request.getHeight() <= 0) {
        throw new IllegalArgumentException("Height must be greater than 0");
    }

    if (request.getWeight() == null || request.getWeight() <= 0) {
        throw new IllegalArgumentException("Weight must be greater than 0");
    }
}
```

Optional helper:

```java
private String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isBlank() ? null : trimmed;
}
```

### 11.3 Cancel precheck

Rules:

- current user must be a nurse
- nurse can cancel only their own precheck
- only `VALID` precheck can be cancelled
- current time must be within appointment time range

Suggested flow:

```java
@Override
@Transactional
public Precheck cancelPrecheck(UUID nurseId, UUID precheckId) {
    userRepository.findByIdAndRole(nurseId, RoleEnum.NURSE)
        .orElseThrow(() -> new NoSuchElementException(
            String.format("NURSE with ID %s not found", nurseId)
        ));

    Precheck precheck = precheckRepository.findByIdAndNurseId(precheckId, nurseId)
        .orElseThrow(() -> new NoSuchElementException(
            String.format("Precheck with ID %s not found", precheckId)
        ));

    if (!PrecheckStatusEnum.VALID.equals(precheck.getStatus())) {
        throw new IllegalArgumentException("Only valid precheck can be cancelled");
    }

    LocalDateTime now = LocalDateTime.now();
    Appointment appointment = precheck.getAppointment();

    if (now.isBefore(appointment.getStartTime()) || now.isAfter(appointment.getEndTime())) {
        throw new IllegalArgumentException("Precheck can only be cancelled within appointment time range");
    }

    precheck.setStatus(PrecheckStatusEnum.CANCELLED);
    return precheckRepository.save(precheck);
}
```

### 11.4 List prechecks for patient

Patient can only see their own prechecks.

Build specification from:

- current patient id
- optional doctor id
- optional nurse id
- optional appointment id
- optional status
- optional created date

### 11.5 List prechecks for doctor

Doctor can directly see only prechecks from appointments where they are the assigned doctor.

### 11.6 List prechecks for nurse

Nurse can see only prechecks they created.

### 11.7 List shared prechecks for another doctor

This must copy the same idea as:

- `PatientRecordAccessServiceImpl`
- `LabServiceImpl.listLabRequestForNewDoctor(...)`

Flow:

1. validate current doctor exists
2. find patient ids from `PatientRecordAccessRepository` where:
   - doctor id = current doctor
   - record type = `PatientRecordTypeEnum.PRECHECK`
   - status = `PatientRecordAccessStatusEnum.APPROVED`
3. if empty, return `Page.empty(pageable)`
4. query prechecks where patient id is in approved list
5. exclude prechecks created for appointments belonging to this same doctor

Why exclude own records:

because doctor already has direct access to their own appointment prechecks

Suggested core logic:

```java
List<UUID> approvedPatientIds = patientRecordAccessRepository
    .findPatientIdsByDoctorIdAndRecordTypeAndStatus(
        doctorId,
        PatientRecordTypeEnum.PRECHECK,
        PatientRecordAccessStatusEnum.APPROVED
    );
```

Then build spec:

```java
spec = spec.and((root, query, cb) ->
    root.get("patient").get("id").in(approvedPatientIds)
);

spec = spec.and((root, query, cb) ->
    cb.notEqual(root.get("doctor").get("id"), doctorId)
);
```

## 12. Helper method for list queries

Just like other services in your project, create a private helper to avoid repeating code.

Suggested idea:

```java
private Page<Precheck> listPrecheckHelper(
    ListPrecheckRequest request,
    Map<RoleEnum, UUID> roleMap,
    Pageable pageable
) {
    Specification<Precheck> spec = PrecheckSpecification.alwaysTrue();

    UUID patientId = roleMap.get(RoleEnum.PATIENT);
    if (patientId != null) spec = spec.and(PrecheckSpecification.byPatient(patientId));

    UUID doctorId = roleMap.get(RoleEnum.DOCTOR);
    if (doctorId != null) spec = spec.and(PrecheckSpecification.byDoctor(doctorId));

    UUID nurseId = roleMap.get(RoleEnum.NURSE);
    if (nurseId != null) spec = spec.and(PrecheckSpecification.byNurse(nurseId));

    if (request.getAppointmentId() != null) {
        spec = spec.and(PrecheckSpecification.byAppointment(request.getAppointmentId()));
    }

    if (request.getStatus() != null) {
        spec = spec.and(PrecheckSpecification.byStatus(request.getStatus()));
    }

    if (request.getCreatedAtDate() != null) {
        spec = spec.and(PrecheckSpecification.byCreatedAtDate(request.getCreatedAtDate()));
    }

    return precheckRepository.findAll(spec, pageable);
}
```

Also validate role existence like the other services do.

## 13. Controller

Follow `LabController` and `PatientRecordAccessController` style.

Suggested endpoints:

### Create

```java
@PostMapping("/create")
@PreAuthorize("hasRole('NURSE')")
public ResponseEntity<PrecheckResponseDto> createPrecheck(
    @AuthenticationPrincipal Jwt jwt,
    @RequestBody @Valid CreatePrecheckRequestDto requestDto
) {
    CreatePrecheckRequest request = precheckMapper.fromCreatePrecheckRequestDto(requestDto);
    Precheck precheck = precheckService.createPrecheck(JwtUtil.getUserId(jwt), request);
    return new ResponseEntity<>(precheckMapper.toPrecheckResponseDto(precheck), HttpStatus.CREATED);
}
```

### Cancel

```java
@PutMapping("/{precheckId}/cancel")
@PreAuthorize("hasRole('NURSE')")
public ResponseEntity<PrecheckResponseDto> cancelPrecheck(
    @AuthenticationPrincipal Jwt jwt,
    @PathVariable UUID precheckId
) {
    Precheck precheck = precheckService.cancelPrecheck(JwtUtil.getUserId(jwt), precheckId);
    return ResponseEntity.ok(precheckMapper.toPrecheckResponseDto(precheck));
}
```

### Normal list

Roles:

- patient
- doctor
- nurse

```java
@PostMapping("/list")
@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'NURSE')")
public ResponseEntity<Page<PrecheckResponseDto>> listPrechecks(
    @AuthenticationPrincipal Jwt jwt,
    Pageable pageable,
    @RequestBody @Valid ListPrecheckRequestDto requestDto
) {
    ListPrecheckRequest request = precheckMapper.fromListPrecheckRequestDto(requestDto);
    UUID requesterId = JwtUtil.getUserId(jwt);
    RoleEnum role = JwtUtil.getRole(jwt);

    Page<Precheck> prechecks;

    switch (role) {
        case PATIENT -> prechecks = precheckService.listPrecheckForPatient(requesterId, request, pageable);
        case DOCTOR -> prechecks = precheckService.listPrecheckForDoctor(requesterId, request, pageable);
        case NURSE -> prechecks = precheckService.listPrecheckForNurse(requesterId, request, pageable);
        default -> throw new IllegalArgumentException(String.format("%s role is not allowed", role.name()));
    }

    return ResponseEntity.ok(prechecks.map(precheckMapper::toPrecheckResponseDto));
}
```

### Shared list for another doctor

```java
@PostMapping("/shared/list")
@PreAuthorize("hasRole('DOCTOR')")
public ResponseEntity<Page<PrecheckResponseDto>> listSharedPrechecks(
    @AuthenticationPrincipal Jwt jwt,
    Pageable pageable,
    @RequestBody @Valid ListPrecheckRequestDto requestDto
) {
    ListPrecheckRequest request = precheckMapper.fromListPrecheckRequestDto(requestDto);
    Page<Precheck> prechecks = precheckService.listSharedPrecheckForDoctor(
        JwtUtil.getUserId(jwt),
        request,
        pageable
    );

    return ResponseEntity.ok(prechecks.map(precheckMapper::toPrecheckResponseDto));
}
```

Use base path:

```java
@RequestMapping(path = "/api/v1/prechecks")
```

## 14. Permission rules summary

### Nurse

- create precheck
- cancel own precheck during appointment time range
- list own prechecks

### Doctor assigned to appointment

- list prechecks for their own appointments

### Patient

- list own prechecks

### Another doctor

- cannot access directly
- must have approved patient record access of type `PRECHECK`

## 15. Database safety for one valid precheck

Service validation is required, but not enough for concurrency.

Problem:

- two requests can arrive at the same time
- both pass `existsByAppointmentIdAndStatus(...)`
- both save a valid precheck

So add DB protection too.

Best solution:

- partial unique index on `appointment_id` where `status = 'VALID'`

Example idea in SQL:

```sql
create unique index uq_prechecks_valid_appointment
on prechecks (appointment_id)
where status = 'VALID';
```

If your database/migration setup does not support partial index easily, still do service validation first, and add DB-level protection later.

## 16. Recommended implementation order for your junior

Tell your junior to build in this order:

1. Create `PrecheckStatusEnum`
2. Create `Precheck` entity
3. Update `User`, `Appointment`, `PatientRecordTypeEnum`
4. Create request objects and DTOs
5. Create `PrecheckMapper`
6. Create `PrecheckRepository`
7. Create `PrecheckSpecification`
8. Create `PrecheckService`
9. Create `PrecheckServiceImpl`
10. Add `validateCreateRequest(request);` inside `createPrecheck(...)`
11. Create `PrecheckController`
12. Test create, cancel, list, and shared doctor access

## 17. Testing checklist

Your junior should manually test these cases:

### Create

- nurse creates precheck for completed appointment -> success
- nurse creates precheck for confirmed appointment -> fail
- nurse creates second valid precheck for same appointment -> fail
- nurse creates precheck with invalid pulse/sugar/temp/height/weight -> fail

### Cancel

- nurse cancels own valid precheck within appointment range -> success
- nurse cancels own valid precheck before appointment start -> fail
- nurse cancels own valid precheck after appointment end -> fail
- nurse cancels already cancelled precheck -> fail
- nurse cancels another nurse's precheck -> fail

### List

- patient sees own prechecks only
- doctor sees own appointment prechecks only
- nurse sees own created prechecks only

### Shared access

- another doctor without approved record access -> no result
- another doctor with approved `PRECHECK` access -> can see shared prechecks
- doctor should not use shared endpoint to get their own appointment prechecks

## 18. Common mistakes to avoid

### Mistake 1

Letting nurse edit the precheck after creation.

Do not do that for now.
Your requirement says if nurse makes a mistake, they cancel and create a new one.

### Mistake 2

Allowing cancellation anytime.

Do not do that.
Cancellation is only allowed during the appointment time window.

### Mistake 3

Checking only appointment id and forgetting status.

The rule is one valid precheck, not one total precheck.
Cancelled prechecks should stay in history.

### Mistake 4

Letting any doctor see all prechecks.

Do not do that.
Only appointment doctor gets direct access.
Other doctors must use patient record access.

### Mistake 5

Forgetting this line in create:

```java
validateCreateRequest(request);
```

## 19. Final recommendation

For this codebase, the cleanest implementation is:

- new `Precheck` entity
- status-based history
- one valid precheck per appointment
- nurse ownership on precheck, not on appointment
- shared doctor access through `PatientRecordAccess` with new type `PRECHECK`

If the project later wants nurse assignment before the visit starts, then you can add `nurse` directly to `Appointment`.
But for this feature alone, that extra change is not necessary.
