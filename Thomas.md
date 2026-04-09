# Prescription Feature Handoff for Thomas

## Goal

Build a prescription feature for doctors that follows the same project structure and access-sharing pattern already used in the lab feature.

The key business rules are:

- A doctor creates a prescription for a specific appointment.
- One prescription can contain multiple medicines.
- Medicine rows only store medicine-related details such as name, dosage, frequency, route, instructions, and quantity.
- Refill information belongs to the parent prescription, not to each medicine row.
- A patient consumes a refill for the whole prescription.
- If a doctor wants to view a patient's old prescriptions that the doctor did not create, the doctor must request access first.
- The patient can approve, reject, or revoke that access.

## How Patient Record Access Works

Do not create a new permission system for prescriptions.

The project already has a reusable access mechanism:

- `PatientRecordAccess`
- `PatientRecordAccessService`
- `PatientRecordAccessRepository`
- `PatientRecordTypeEnum.PRESCRIPTION`

Use that existing flow:

1. A doctor sends an access request to a patient using `PatientRecordAccessService.requestPatientRecordAccess(...)`.
2. The request uses `recordType = PRESCRIPTION`.
3. A `PatientRecordAccess` row is created with:
   - doctor
   - patient
   - record type
   - status `PENDING`
4. The patient can:
   - approve using `PatientRecordAccessService.approve(...)`
   - deny using `PatientRecordAccessService.deny(...)`
   - revoke later using `PatientRecordAccessService.revoke(...)`
5. When implementing "doctor views prescriptions they did not create", only return prescriptions for patients whose access request is:
   - for `PRESCRIPTION`
   - approved
   - tied to that doctor

This is the same idea already used for shared lab request viewing.

## Data Model

Use a parent-child structure:

- `Prescription` is the parent record
- `PrescriptionMedicine` is the child row for each medicine

### Prescription

This object should hold:

- `id`
- `status`
- `doctor`
- `patient`
- `appointment`
- `startDate`
- `endDate`
- `totalRefills`
- `remainingRefills`
- `refillIntervalWeeks`
- `nextEligibleRefillAt`
- `lastConsumedAt`
- `generalNote`
- `createdAt`
- `lastUpdated`
- `version`
- `medicines`

Suggested status enum:

- `ACTIVE`
- `COMPLETED`
- `CANCELLED`
- `EXPIRED`

### PrescriptionMedicine

This object should only hold medicine information:

- `id`
- `prescription`
- `medicineName`
- `dosage`
- `frequency`
- `route`
- `instructions`
- `quantity`
- `createdAt`
- `lastUpdated`

Important:

- Do not put refill fields on `PrescriptionMedicine`.
- Do not put `remainingRefills` on `PrescriptionMedicine`.
- Do not put `nextEligibleRefillAt` on `PrescriptionMedicine`.
- Refill state belongs to `Prescription`.

## Relationships

Do not forget to wire the new entities back into existing entities too. The prescription feature should not exist in isolation.

### In Prescription

Add:

- `ManyToOne` to `User doctor`
- `ManyToOne` to `User patient`
- `ManyToOne` to `Appointment appointment`
- `OneToMany` to `PrescriptionMedicine`

### In PrescriptionMedicine

Add:

- `ManyToOne` to `Prescription`

### In User

Add relationships for prescriptions too if you want consistency with the rest of the model.

Recommended:

- prescriptions created by doctor
- prescriptions owned by patient

### In Appointment

Also add the relationship to prescription.

Decide early whether the business rule is:

- one appointment can have many prescriptions
- one appointment can have only one prescription

Recommended first version:

- `Appointment -> OneToMany<Prescription>`

This is more flexible.

## Build Order

Follow this order so each step has the classes it depends on.

1. Create `PrescriptionStatusEnum`
2. Create `Prescription` entity
3. Create `PrescriptionMedicine` entity
4. Add entity relationships:
   - `Prescription` to `User`
   - `Prescription` to `Appointment`
   - `Prescription` to `PrescriptionMedicine`
   - update `User`
   - update `Appointment`
5. Create domain request objects
6. Create request DTOs
7. Create response DTOs
8. Create mapper
9. Create repository
10. Create specification
11. Create service interface
12. Implement service logic
13. Create controller
14. Add validation and test cases

## Domain Request Objects

Create backend request objects for service-layer input.

Use:

- `CreatePrescriptionRequest`
- `CreatePrescriptionMedicineRequest`
- `ListPrescriptionRequest`

Do not create a request object for consume refill.

### CreatePrescriptionRequest

Suggested fields:

- `appointmentId`
- `startDate`
- `endDate`
- `totalRefills`
- `refillIntervalWeeks`
- `generalNote`
- `List<CreatePrescriptionMedicineRequest> medicines`

### CreatePrescriptionMedicineRequest

Suggested fields:

- `medicineName`
- `dosage`
- `frequency`
- `route`
- `instructions`
- `quantity`

### ListPrescriptionRequest

Suggested fields:

- `patientId`
- `doctorId`
- `appointmentId`
- `status`
- `medicineName`
- `createdAtDate`

You can keep this simple at first and extend filters later.

## Request DTOs

Create API request DTOs under the DTO package.

Use:

- `CreatePrescriptionRequestDto`
- `CreatePrescriptionMedicineRequestDto`
- `ListPrescriptionRequestDto`

Do not create a request DTO for consume refill.

Why:

- consume refill only needs authenticated `patientId`
- and the `prescriptionId` path variable

That is enough.

## Response DTOs

Create response DTOs for returning prescription data cleanly to the frontend.

Use:

- `PrescriptionResponseDto`
- `PrescriptionMedicineResponseDto`

### PrescriptionResponseDto

Suggested fields:

- `id`
- `doctorId`
- `patientId`
- `appointmentId`
- `status`
- `startDate`
- `endDate`
- `totalRefills`
- `remainingRefills`
- `refillIntervalWeeks`
- `nextEligibleRefillAt`
- `lastConsumedAt`
- `generalNote`
- `createdAt`
- `List<PrescriptionMedicineResponseDto> medicines`

### PrescriptionMedicineResponseDto

Suggested fields:

- `id`
- `medicineName`
- `dosage`
- `frequency`
- `route`
- `instructions`
- `quantity`

## Mapper

Create a mapper to handle conversions between DTOs, request objects, and entities.

Suggested mapper:

- `PrescriptionMapper`

Mapper responsibilities:

- `CreatePrescriptionRequestDto -> CreatePrescriptionRequest`
- `CreatePrescriptionMedicineRequestDto -> CreatePrescriptionMedicineRequest`
- `ListPrescriptionRequestDto -> ListPrescriptionRequest`
- `Prescription -> PrescriptionResponseDto`
- `PrescriptionMedicine -> PrescriptionMedicineResponseDto`

The mapper should also handle the nested medicine list conversion.

## Repository

Create:

- `PrescriptionRepository`

It should extend:

- `JpaRepository<Prescription, UUID>`
- `JpaSpecificationExecutor<Prescription>`

Useful methods:

- `findByIdAndPatientId(...)`
- `findByIdAndDoctorId(...)`

If later needed, add duplicate-checking methods too.

You probably do not need a separate complex repository for `PrescriptionMedicine` in the first version unless you decide to query medicine rows independently.

## Specification

Create:

- `PrescriptionSpecification`

Suggested filters:

- `alwaysTrue()`
- `byPatient(...)`
- `byDoctor(...)`
- `byAppointment(...)`
- `byStatus(...)`
- `byCreatedAtDate(...)`
- maybe a medicine-name filter using a join to medicines

This keeps list endpoints clean and consistent with the rest of the codebase.

## Service Interface

Create:

- `PrescriptionService`

Suggested methods:

- `createPrescription(UUID doctorId, CreatePrescriptionRequest request)`
- `cancelPrescription(UUID doctorId, UUID prescriptionId)`
- `consumeRefill(UUID patientId, UUID prescriptionId)`
- `listPrescriptionForDoctor(UUID doctorId, ListPrescriptionRequest request, Pageable pageable)`
- `listPrescriptionForPatient(UUID patientId, ListPrescriptionRequest request, Pageable pageable)`
- `listPrescriptionForNewDoctor(UUID doctorId, ListPrescriptionRequest request, Pageable pageable)`

Important:

- `consumeRefill(...)` should not take a request body
- just use `patientId` and `prescriptionId`

## Service Logic

### createPrescription

Rules:

- validate doctor exists and has role `DOCTOR`
- find appointment
- ensure appointment exists
- ensure the appointment belongs to that doctor
- ensure the appointment status allows prescription creation, probably only after completion
- patient should come from the appointment, not from free-form input
- create the parent `Prescription`
- create nested `PrescriptionMedicine` rows
- initialize refill-related fields on `Prescription`

Suggested initialization:

- `status = ACTIVE`
- `remainingRefills = totalRefills`
- `nextEligibleRefillAt = startDate.atStartOfDay()` or another clearly defined rule

### consumeRefill

Rules:

- validate patient exists
- find prescription by `prescriptionId` and `patientId`
- status must allow refill consumption
- current time must not be after `endDate`
- `remainingRefills` must be greater than `0`
- current time must be on or after `nextEligibleRefillAt`
- decrease `remainingRefills`
- set `lastConsumedAt = now`
- if current time is after `endDate`, reject the refill and optionally mark the prescription as `EXPIRED`
- if refill remains:
  - update `nextEligibleRefillAt = now.plusWeeks(refillIntervalWeeks)`
- if no refill remains:
  - mark prescription as `COMPLETED`

Again:

- do not consume by medicine id
- consume by prescription id

### listPrescriptionForDoctor

Return only prescriptions created by that doctor, with optional filters.

### listPrescriptionForPatient

Return only prescriptions belonging to that patient, with optional filters.

### listPrescriptionForNewDoctor

This is the shared-view flow.

Rules:

- validate doctor exists
- ask `PatientRecordAccessRepository` for approved patient ids where:
  - doctor id matches
  - record type is `PRESCRIPTION`
  - status is `APPROVED`
- if none, return empty page
- return only prescriptions for those patients
- optionally exclude prescriptions created by the same doctor if the feature is specifically for "prescriptions I did not create"

This should follow the same pattern as the lab shared-access logic.

## Controller

Create:

- `PrescriptionController`

Suggested endpoints:

### Doctor endpoints

- `POST /api/prescriptions`
- `GET /api/doctor/prescriptions`
- `GET /api/doctor/shared-prescriptions`
- `POST /api/prescriptions/{id}/cancel`

### Patient endpoints

- `GET /api/patient/prescriptions`
- `POST /api/prescriptions/{id}/consume-refill`

## Endpoint Workflow

### Create Prescription Flow

1. Controller receives `CreatePrescriptionRequestDto`
2. Controller gets authenticated doctor id
3. Mapper converts DTO to `CreatePrescriptionRequest`
4. Service creates prescription
5. Mapper converts entity to `PrescriptionResponseDto`
6. Controller returns response

### List Prescription Flow

1. Controller receives `ListPrescriptionRequestDto`
2. Controller gets authenticated user id
3. Mapper converts DTO to `ListPrescriptionRequest`
4. Service runs filtered query
5. Mapper converts entities to response DTOs
6. Controller returns paginated result

### Consume Refill Flow

1. Controller gets authenticated patient id
2. Controller gets `prescriptionId` from path variable
3. Controller calls `PrescriptionService.consumeRefill(patientId, prescriptionId)`
4. Mapper converts updated entity to `PrescriptionResponseDto`
5. Controller returns response

No request body is needed for this endpoint.

## Validation Notes

Add validation for:

- appointment must exist
- appointment must belong to the doctor creating the prescription
- appointment status must allow prescription creation
- medicine list should not be empty
- total refills should not be negative
- refill interval should be valid
- end date should not be before start date
- patient cannot consume refill early
- patient cannot consume refill after the prescription end date
- patient cannot consume refill when no refill remains

## Final Reminder

The most important implementation points are:

- keep refill state on `Prescription`
- keep medicine details on `PrescriptionMedicine`
- do not create a request DTO or request object for consume refill
- consume refill by `prescriptionId`
- reuse `PatientRecordAccess` for prescription sharing
- do not forget to add relationships in `User` and `Appointment` too

If you follow the existing project patterns used in lab requests, patient record access, repository specifications, mapper classes, and service/controller layering, the prescription feature should fit naturally into the codebase.
