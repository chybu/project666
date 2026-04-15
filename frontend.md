# Frontend Design Document

## 1. Overview

This document defines the server-rendered frontend design based on the current backend service layer and service implementations. I will explain how the system works, general rules, and how to use the backend service.

Primary rule for this frontend:

- The frontend should call backend services directly from frontend controllers.
- The REST controllers are listed side by side only as reference or fallback.
- Every page design below is based on the actual service implementation, including validation rules, filters, and status transitions.
- When the user provides an invalid parameter, the backend will throw an error. This error can be handled in the frontend by catching the error and displaying the error message, so there is no need to perform parameter validation on the frontend.
- See workflow.md as a reference
This document covers these six stakeholders:

- Patient
- Doctor
- Nurse
- Lab technician
- Accountant
- Receptionist

## 2. SSR Integration Model

### 2.1 Primary integration style

For server-rendered pages, inject backend services directly into frontend controllers and call them with the logged-in user's UUID.

Typical SSR flow:

1. Get the current user UUID from `OidcUser`.
2. Build the backend request object.
3. Build a `Pageable`.
4. Call the service directly.
5. Put the returned data into the `Model`.
6. Render the Thymeleaf template.

Example pattern:

```java
UUID patientId = OidcUserUtil.getUserId(oidcUser);

ListAppointmentRequest request = new ListAppointmentRequest();
request.setStatus(AppointmentStatusEnum.CONFIRMED);
request.setFrom(LocalDate.now());

Pageable pageable = PageRequest.of(0, 5, Sort.by("startTime").ascending());

Page<Appointment> appointmentPage =
    appointmentService.listAppointmentForPatient(patientId, request, pageable);

model.addAttribute("appointments", appointmentPage.getContent());
```

### 2.2 Why the service layer is preferred

- The service layer already enforces business rules.
- The service layer already scopes data by role and current user.
- The service layer returns domain objects directly, which fits SSR well.
- The REST controller layer adds DTO mapping and JWT extraction, which is not needed inside the same application.

### 2.3 General frontend rules

- Always use the current logged-in user ID for the first method parameter when the service requires it.
- Only set request filter fields that the page actually needs.
- Use `Pageable` on every list page.
- Sort by the most user-meaningful field for the page:
  - appointments: usually `startTime`
  - bills: usually `createdAt` or `paidOn`
  - prescriptions: usually `createdAt`
  - lab requests and lab tests: usually `createdAt`
- Show empty states explicitly. Many services return `Page.empty(pageable)` when nothing matches.
- Surface business-rule failures as user-friendly error messages.

### 2.4 Controller-equivalent note

Every feature below lists:

- direct service call for SSR
- controller equivalent endpoint

The controller equivalent is useful when:

- the frontend is later split into a separate app
- some AJAX feature is introduced

### 2.5 Current routing note

The current frontend callback route sends:

- patient to `/patient/dashboard/home`
- doctor to `/doctor/dashboard/home`
- receptionist to `/receptionist/dashboard/home`
- nurse to `/nurse/dashboard/home`
- accountant to `/accountant/dashboard/home`

There is already a lab technician frontend controller at `/labtechnician/...`, but the current callback flow does not redirect `LAB_TECHNICIAN` yet. This document still includes lab technician pages because that stakeholder is part of the required design.

## 3. Shared Business Rules the Frontend Must Respect

### 3.1 Appointment creation rules

These rules are enforced by `AppointmentServiceImpl`:

- `startTime` is required.
- `type` is required.
- `doctorId` is required.
- Patient self-booking doesn't require `request.patientId`.
- Receptionist booking still needs `request.patientId`, because the service reads it when the creator is not a patient.
- Appointment must be at least 3 days in the future.
- Appointment must be no more than 31 days in the future.
- Appointment must fit inside working hours `08:00` to `18:00`.
- Appointment duration depends on type:
  - `QUICK_CHECK`: 30 minutes plus 15-minute precheck buffer
  - `MID_CHECK`: 60 minutes plus 15-minute precheck buffer
  - `LONG_CHECK`: 90 minutes plus 15-minute precheck buffer
- Doctor overlap is blocked. (Doctor cannot have two appointments at the same time)
- Patient overlap is blocked. (Patient cannot have two appointments at the same time)
- Overlap check includes a 30-minute buffer before and after the appointment window. (Each appointment has a 30-minute interval)

### 3.2 Appointment confirmation and operational rules

- Receptionist confirmation only works for appointments in `CONFIRMED` status.
- Confirmation window is from 5 minutes before start time until 15 minutes after start time.
- Confirmation changes status from `CONFIRMED` to `COMPLETED`.
- Confirming an appointment generates the main appointment bill.
- If confirmation happens more than 5 minutes after start time, a late-fee bill is also generated.
- Receptionist no-show can only happen after appointment end time.
- No-show changes status to `NO_SHOW` and generates a cancellation fee bill.

### 3.3 Appointment cancellation rules

- Only a patient or receptionist can cancel.
- Only `CONFIRMED` appointments can be cancelled.
- `CancellationInitiatorEnum.PATIENT` requires the caller role to be `PATIENT`.
- `CancellationInitiatorEnum.RECEPTIONIST` or `RECEPTIONIST_ON_BEHALF_OF_PATIENT` requires the caller role to be `RECEPTIONIST`.
- A cancellation fee is only generated when:
  - initiator is `PATIENT`, or
  - initiator is `RECEPTIONIST_ON_BEHALF_OF_PATIENT`
  - `PATIENT` can have free cancellation 3 days before the appointment start

### 3.4 Precheck rules

- Only nurses create or cancel prechecks.
- Precheck creation requires:
  - valid nurse
  - valid appointment
  - appointment status `COMPLETED`
  - current time must be inside the appointment time range
  - no existing `VALID` precheck for that appointment
- Precheck vitals must all be greater than 0.
- If `NURSE` creates a wrong Precheck, they can cancel a new one and create a new one for that appointment
- Precheck cancel only works on the nurse's own precheck.
- Only `VALID` prechecks can be cancelled.
- Cancel also must happen inside the appointment time range.

### 3.5 Lab workflow rules

- Only doctors create or cancel lab requests.
- Lab request includes multiple lab tests
- Lab request creation requires:
  - valid doctor
  - appointment belongs to that doctor
  - appointment status `COMPLETED`
  - at least one lab test
  - every lab test must have a name
- Duplicate lab test names are blocked:
  - duplicate inside the same request
  - duplicate active test for the same patient in `REQUESTED` or `IN_PROGRESS` (like if another `Doctor` already created a lab request having a lab test with the same name as the new lab request, then the lab test in the new lab request will be invalid and throw an error)
- Lab technician claim only works on tests in `REQUESTED`.
- Lab technician update only works on tests in `IN_PROGRESS`.
- Lab technician submit only works on tests in `IN_PROGRESS`.
- Submit requires a non-blank `result`.
- Completing a lab test generates a lab bill.
- When all tests in a lab request are completed, the parent lab request becomes `COMPLETED`.

### 3.6 Prescription rules

- Only doctors create or cancel prescriptions.
- Prescription creation requires:
  - appointment belongs to that doctor
  - appointment status `COMPLETED`
  - at least one medicine
  - `startDate` and `endDate`
  - `endDate` cannot be before `startDate`
  - `totalRefills >= 0`
  - `refillIntervalDays >= 0`
  - each medicine must have `medicineName`
- Prescription starts in `ACTIVE`.
- `remainingRefills` starts equal to `totalRefills`.
- `nextEligibleRefillAt` starts at `startDate.atStartOfDay()`.
- Patient refill consumption only works when:
  - prescription belongs to that patient
  - status is `ACTIVE`
  - remaining refills are greater than 0
  - current time is on or after `nextEligibleRefillAt`
  - prescription has not expired
- If the current date is after `endDate`, the service marks the prescription `EXPIRED` and throws an error.

### 3.7 Patient-record-access rules

- Only doctors request access.
- Patient-record-access request requires:
  - `patientId`
  - `type`
- Duplicate request is blocked when the same doctor already has a `PENDING` or `APPROVED` request for the same patient and record type.
- Patient can:
  - approve a `PENDING` request
  - deny a `PENDING` request
  - revoke an `APPROVED` request
- Doctor can cancel a `PENDING` request.

### 3.8 Billing rules

- Bills are generated automatically by service actions.
- The frontend should not call bill-generation methods directly.
- Newly generated bills start in `VIEWING`.
- Insurance processing later moves them to `UNPAID`.
- Accountant can only confirm payment when the bill is not `VIEWING` and not already `PAID`.
- Patient pages are read-only for bills. There is no patient payment-confirmation service.

## 4. Stakeholder Design

## 4.1 Patient

### Main goals

- View upcoming appointments
- Review appointment history
- Cancel upcoming appointments
- View unpaid bills
- View prescriptions and consume eligible refills
- View medical artifacts created from attended appointments
- Approve, deny, or revoke doctor access requests for shared medical records

### Recommended pages

- `/patient/dashboard/home`
- `/patient/dashboard/reviewAppointments`
- `/patient/dashboard/finances`
- `/patient/dashboard/pharmacy`
- `/patient/dashboard/records`
- `/patient/dashboard/access-requests`

### Patient home: upcoming appointments

Direct service:

```java
appointmentService.listAppointmentForPatient(patientId, request, pageable)
```

Controller equivalent:

- `POST /api/v1/appointments/list`
- Allowed role: `PATIENT`

Parameters:

- `patientId`: current logged-in patient UUID
- `request.doctorId`: optional
- `request.type`: optional
- `request.status`: usually `AppointmentStatusEnum.CONFIRMED`
- `request.from`: usually `LocalDate.now()`
- `request.end`: optional
- `pageable`: recommended `PageRequest.of(0, 5, Sort.by("startTime").ascending())`

Why this works:

- The service always scopes visibility to the passed `patientId`.
- `doctorId` can narrow the list to one doctor.
- `status = CONFIRMED` removes completed, cancelled, and no-show appointments.
- `from = today` avoids showing already-started past appointments on the home page.

Frontend behavior notes:

- Show date, time, type, doctor name, and status.
- Empty state: "No upcoming appointments."

### Patient review appointments page

Direct service:

```java
appointmentService.listAppointmentForPatient(patientId, request, pageable)
```

Controller equivalent:

- `POST /api/v1/appointments/list`

Recommended filters:

- Upcoming tab:
  - `status = CONFIRMED`
  - `from = LocalDate.now()`
- Past/previous tab:
  - no status filter, `end = LocalDate.now().minusDays(1)`, or
  - separate tabs for `COMPLETED`, `CANCELLED`, and `NO_SHOW`

Important backend behavior:

- Patient cannot see other patients' appointments by changing request fields.
- Date filters are optional.
- General list helper does not reject reversed date range, so the frontend should validate `from <= end` before calling for normal patient pages.

### Patient cancel appointment

Direct service:

```java
appointmentService.cancelAppointment(patientId, RoleEnum.PATIENT, request)
```

Controller equivalent:

- `PUT /api/v1/appointments/cancel`
- Allowed roles: `PATIENT`, `RECEPTIONIST`

Parameters:

- `request.appointmentId`: required
- `request.cancelReason`: optional but recommended
- `request.cancellationInitiator = CancellationInitiatorEnum.PATIENT`

Important backend behavior:

- Appointment must exist.
- Appointment must still be `CONFIRMED`.
- Patient cannot use receptionist initiator values.
- Late cancellation fee bill is generated if the patient cancels later than 3 days before the appointment.

Frontend behavior notes:

- Show a warning before cancellation:
  - "You may be charged a cancellation fee if this is within 3 days of the appointment."

### Patient finances page: appointment bills

Direct service:

```java
billService.listAppointmentBillForPatient(patientId, request, pageable)
```

Controller equivalent:

- `POST /api/v1/bills/appointments/list`
- Allowed roles: `PATIENT`, `ACCOUNTANT`

Parameters:

- `patientId`: current patient
- `request.status`: usually `BillStatusEnum.UNPAID` for outstanding items
- `request.type`: optional
- `request.appointmentId`: optional
- `request.createdAtDate`: optional
- `request.paidOnDate`: optional
- `request.minAmount`, `request.maxAmount`: optional
- `request.confirmAccountantId`: optional

Important backend behavior:

- Amount ranges are validated.
- If `minAmount > maxAmount`, service throws an error.
- Same rule applies to insurance-cover and patient-payment ranges.
- Bills in `VIEWING` are still being processed by insurance and are not ready for payment confirmation.

Frontend behavior notes:

- Recommended tabs:
  - Processing: `status = VIEWING`
  - Unpaid: `status = UNPAID`
  - Paid: `status = PAID`

### Patient finances page: lab bills

Direct service:

```java
billService.listLabBillForPatient(patientId, request, pageable)
```

Controller equivalent:

- `POST /api/v1/bills/labs/list`

Parameters:

- same shared bill filters as appointment bills
- `request.labTestId`: optional

### Patient pharmacy page: prescriptions

Direct service:

```java
prescriptionService.listPrescriptionForPatient(patientId, request, pageable)
```

Controller equivalent:

- `POST /api/v1/prescriptions/list`
- Allowed roles: `PATIENT`, `DOCTOR`

Parameters:

- `patientId`: current patient
- `request.doctorId`: optional
- `request.appointmentId`: optional
- `request.status`: commonly `PrescriptionStatusEnum.ACTIVE`
- `request.medicineName`: optional
- `request.createdAtDate`: optional

Important backend behavior:

- Patient can only see their own prescriptions.
- `medicineName` is supported as a filter.
- `appointmentId` filter is safe because the service still constrains records by patient.

Frontend behavior notes:

- Recommended tabs:
  - Active
  - Completed
  - Cancelled
  - Expired

### Patient pharmacy page: consume refill

Direct service:

```java
prescriptionService.consumeRefill(patientId, prescriptionId)
```

Controller equivalent:

- `PUT /api/v1/prescriptions/{prescriptionId}/consume-refill`
- Allowed role: `PATIENT`

Important backend behavior:

- Prescription must belong to current patient.
- Status must be `ACTIVE`.
- Remaining refills must be greater than 0.
- Current time must be on or after `nextEligibleRefillAt`.
- If already past `endDate`, the service marks the prescription `EXPIRED` and throws an error.

Frontend behavior notes:

- Disable the refill button when:
  - status is not `ACTIVE`
  - `remainingRefills <= 0`
  - current time is before `nextEligibleRefillAt`
- If the frontend has access to `nextEligibleRefillAt`, show it to the user.

### Patient records page: prechecks

Direct service:

```java
precheckService.listPrecheckForPatient(patientId, request, pageable)
```

Controller equivalent:

- `POST /api/v1/prechecks/list`
- Allowed roles: `PATIENT`, `DOCTOR`, `NURSE`

Parameters:

- `patientId`: current patient
- `request.appointmentId`: optional
- `request.doctorId`: optional
- `request.nurseId`: optional
- `request.status`: optional
- `request.createdAtDate`: optional

### Patient records page: lab requests

Direct service:

```java
labService.listLabRequestForPatient(patientId, request, pageable)
```

Controller equivalent:

- `POST /api/v1/labs/requests/list`
- Allowed roles: `PATIENT`, `DOCTOR`, `LAB_TECHNICIAN`

Important backend behavior:

- Patient version returns `PatientLabRequestResponseDto`, not raw `LabRequest`.
- This hides staff-only note fields from the patient.

### Patient access-requests page

List access requests:

```java
patientRecordAccessService.listPatientRecordAccess(patientId, request, pageable)
```

Approve request:

```java
patientRecordAccessService.approve(patientId, accessId)
```

Deny request:

```java
patientRecordAccessService.deny(patientId, accessId)
```

Revoke access:

```java
patientRecordAccessService.revoke(patientId, accessId)
```

Controller equivalents:

- `POST /api/v1/patient-record-access/list`
- `PUT /api/v1/patient-record-access/{id}/approve`
- `PUT /api/v1/patient-record-access/{id}/deny`
- `PUT /api/v1/patient-record-access/{id}/revoke`

Important backend behavior:

- Approve and deny only work from `PENDING`.
- Revoke only works from `APPROVED`.

## 4.2 Doctor

### Main goals

- View own appointments
- Create and manage prescriptions
- Create and manage lab requests
- Review direct prechecks for own appointments
- Review shared records from other doctors when access is approved
- Request access to patient records

### Recommended pages

- `/doctor/dashboard/home`
- `/doctor/dashboard/appointments`
- `/doctor/dashboard/prescriptions`
- `/doctor/dashboard/labs`
- `/doctor/dashboard/prechecks`
- `/doctor/dashboard/shared-records`
- `/doctor/dashboard/access-requests`

### Doctor home or appointments page: own appointments

Direct service:

```java
appointmentService.listAppointmentForDoctor(doctorId, request, pageable)
```

Controller equivalent:

- `POST /api/v1/appointments/list`
- Allowed roles: `PATIENT`, `DOCTOR`, `RECEPTIONIST`, `NURSE`

Parameters:

- `doctorId`: current doctor
- `request.patientId`: optional
- `request.type`: optional
- `request.status`: optional
- `request.from`: optional
- `request.end`: optional

Recommended uses:

- Upcoming clinic schedule:
  - `status = CONFIRMED`
  - `from = LocalDate.now()`
- Completed visit history:
  - `status = COMPLETED`

### Doctor direct prechecks for own appointments

Direct service:

```java
precheckService.listPrecheckForDoctor(doctorId, request, pageable)
```

Controller equivalent:

- `POST /api/v1/prechecks/list`

Parameters:

- `doctorId`: current doctor
- `request.patientId`: optional
- `request.nurseId`: optional
- `request.appointmentId`: optional
- `request.status`: usually `PrecheckStatusEnum.VALID`
- `request.createdAtDate`: optional

### Doctor create prescription

Direct service:

```java
prescriptionService.createPrescription(doctorId, request)
```

Controller equivalent:

- `POST /api/v1/prescriptions`
- Allowed role: `DOCTOR`

Required request fields:

- `appointmentId`
- `startDate`
- `endDate`
- `totalRefills`
- `refillIntervalDays`
- `medicines`

Each medicine needs:

- `medicineName`
- optional `dosage`
- optional `frequency`
- optional `route`
- optional `instructions`
- optional `quantity`

Important backend behavior:

- Appointment must belong to the current doctor.
- Appointment must be `COMPLETED`.
- Prescription must contain at least one medicine.
- `endDate` cannot be before `startDate`.
- `totalRefills` and `refillIntervalDays` cannot be negative.

Frontend behavior notes:

- Do not show the prescription form for appointments that are still only `CONFIRMED`.

### Doctor list and cancel prescriptions

List:

```java
prescriptionService.listPrescriptionForDoctor(doctorId, request, pageable)
```

Cancel:

```java
prescriptionService.cancelPrescription(doctorId, prescriptionId)
```

Controller equivalents:

- `POST /api/v1/prescriptions/list`
- `PUT /api/v1/prescriptions/{prescriptionId}/cancel`

Important backend behavior:

- Doctor can only cancel prescriptions they created.
- Cannot cancel `CANCELLED`.
- Cannot cancel `COMPLETED`.

### Doctor create lab request

Direct service:

```java
labService.createLabRequest(doctorId, request)
```

Controller equivalent:

- `POST /api/v1/labs/requests/create`
- Allowed role: `DOCTOR`

Required request fields:

- `appointmentId`
- `labTests`

Each lab test should include:

- `name`
- optional `code`
- optional `unit`
- optional `doctorNote`

Important backend behavior:

- Appointment must belong to current doctor.
- Appointment must be `COMPLETED`.
- Request must contain at least one lab test.
- Every test must have a non-blank name.
- Duplicate names inside the same request are blocked.
- Duplicate active tests for the same patient are blocked.

Frontend behavior notes:

- This page should preferably be launched from a completed appointment detail page.

### Doctor list and cancel lab requests

List:

```java
labService.listLabRequestForDoctor(doctorId, request, pageable)
```

Cancel:

```java
labService.cancelLabRequest(doctorId, requestId)
```

Controller equivalents:

- `POST /api/v1/labs/requests/list`
- `PUT /api/v1/labs/requests/{requestId}/cancel`

Important backend behavior:

- Doctor can only cancel their own request.
- Cannot cancel `COMPLETED`.
- Cannot cancel already `CANCELLED`.
- Cancelling a request also marks all child lab tests `CANCELLED`.

### Doctor request patient-record access

Direct service:

```java
patientRecordAccessService.requestPatientRecordAccess(doctorId, request)
```

Controller equivalent:

- `POST /api/v1/patient-record-access/request`
- Allowed role: `DOCTOR`

Required request fields:

- `patientId`
- `type`

Important backend behavior:

- Duplicate `PENDING` or `APPROVED` access requests for the same patient and type are blocked.

Recommended UI:

- Let the doctor request access by record type:
  - `PRECHECK`
  - `LAB_REQUEST`
  - `PRESCRIPTION`

### Doctor shared-records page

Shared access requests:

```java
patientRecordAccessService.listSharedPatientRecordAccess(doctorId, request, pageable)
```

Shared prechecks:

```java
precheckService.listSharedPrecheckForDoctor(doctorId, request, pageable)
```

Shared lab requests:

```java
labService.listLabRequestForNewDoctor(doctorId, request, pageable)
```

Shared prescriptions:

```java
prescriptionService.listPrescriptionForNewDoctor(doctorId, request, pageable)
```

Controller equivalents:

- `POST /api/v1/patient-record-access/list`
- `POST /api/v1/prechecks/shared/list`
- `POST /api/v1/labs/requests/shared/list`
- `POST /api/v1/prescriptions/shared/list`

Important backend behavior:

- Shared record services only return patients whose access is `APPROVED`.
- Shared doctor views exclude records originally created by the same doctor.

### Doctor cancel access request

Direct service:

```java
patientRecordAccessService.cancel(doctorId, accessId)
```

Controller equivalent:

- `PUT /api/v1/patient-record-access/{id}/cancel`

Important backend behavior:

- Doctor can only cancel a `PENDING` request.

## 4.3 Nurse

### Main goals

- View the nurse operational queue
- Create prechecks for attended appointments
- Cancel own prechecks while still inside the appointment time range
- Review own created prechecks

### Recommended pages

- `/nurse/dashboard/home`
- `/nurse/dashboard/appointments`
- `/nurse/dashboard/prechecks`

### Nurse queue: upcoming completed appointments

Direct service:

```java
appointmentService.listAppointmentForNurse(nurseId, request, pageable)
```

Controller equivalent:

- `POST /api/v1/appointments/list`
- Allowed roles: `PATIENT`, `DOCTOR`, `RECEPTIONIST`, `NURSE`

Parameters:

- `nurseId`: current nurse
- `request.patientId`: optional
- `request.doctorId`: optional
- `request.type`: optional
- `request.from`: optional
- `request.end`: optional

Important backend behavior:

- The implementation hardcodes `status = COMPLETED`.
- The implementation also requires `endTime >= now`.
- `request.status` is ignored in this nurse-specific flow.
- If both `from` and `end` are set and `end < from`, service returns an empty page.

Recommended UI interpretation:

- This queue represents appointments that were checked in and are still active for nurse work, not future unconfirmed appointments.

### Nurse create precheck

Direct service:

```java
precheckService.createPrecheck(nurseId, request)
```

Controller equivalent:

- `POST /api/v1/prechecks/create`
- Allowed role: `NURSE`

Required request fields:

- `appointmentId`
- `pulse`
- `sugar`
- `temperature`
- `height`
- `weight`
- optional `note`

Important backend behavior:

- Every numeric vital must be greater than 0.
- Appointment must exist.
- Appointment must be `COMPLETED`.
- Current time must be between appointment start and end.
- There can only be one `VALID` precheck per appointment.

Frontend behavior notes:

- Only show the precheck form on appointments that come from the nurse queue.

### Nurse cancel precheck

Direct service:

```java
precheckService.cancelPrecheck(nurseId, precheckId)
```

Controller equivalent:

- `PUT /api/v1/prechecks/{precheckId}/cancel`
- Allowed role: `NURSE`

Important backend behavior:

- Nurse can only cancel their own precheck.
- Precheck must be `VALID`.
- Cancellation must happen inside the appointment time range.

### Nurse list own prechecks

Direct service:

```java
precheckService.listPrecheckForNurse(nurseId, request, pageable)
```

Controller equivalent:

- `POST /api/v1/prechecks/list`

Parameters:

- `request.patientId`: optional
- `request.doctorId`: optional
- `request.appointmentId`: optional
- `request.status`: optional
- `request.createdAtDate`: optional

## 4.4 Lab Technician

### Main goals

- View unfinished lab requests
- View tests assigned to self
- Claim a requested test
- Update a claimed test
- Submit a completed test result

### Recommended pages

- `/labtechnician/dashboard/home`
- `/labtechnician/dashboard/lab-requests`
- `/labtechnician/dashboard/lab-tests`

### Lab technician request queue

Direct service:

```java
labService.listLabRequestForLabTechnician(labTechnicianId, request, pageable)
```

Controller equivalent:

- `POST /api/v1/labs/requests/list`
- Allowed roles: `PATIENT`, `DOCTOR`, `LAB_TECHNICIAN`

Parameters:

- `labTechnicianId`: current technician
- `request.patientId`: optional
- `request.doctorId`: optional
- `request.createdAtDate`: optional

Important backend behavior:

- This method automatically restricts to unfinished requests only.
- It is suitable for a shared work queue.

Recommended UI:

- Show request status, patient, doctor, created time, and number of tests.
- Provide a path from request detail to individual test claim actions.

### Lab technician list own tests

Direct service:

```java
labService.listLabTestForLabTechnician(labTechnicianId, request, pageable)
```

Controller equivalent:

- `POST /api/v1/labs/tests/list`
- Allowed role: `LAB_TECHNICIAN`

Parameters:

- `labTechnicianId`: current technician
- `request.patientId`: optional
- `request.doctorId`: optional
- `request.status`: optional
- `request.code`: optional
- `request.name`: optional
- `request.unit`: optional
- `request.createdAtDate`: optional

Important backend behavior:

- This list only returns tests already assigned to the current lab technician.

Recommended UI tabs:

- In progress
- Completed
- Cancelled

### Lab technician claim test

Direct service:

```java
labService.claimLabTest(labTechnicianId, labTestId)
```

Controller equivalent:

- `PUT /api/v1/labs/tests/{labTestId}/claim`
- Allowed role: `LAB_TECHNICIAN`

Important backend behavior:

- Test must be `REQUESTED`.
- Claiming assigns the current technician to the test.
- If the parent request was `REQUESTED`, it becomes `IN_PROGRESS`.

Frontend behavior notes:

- Only show claim action on tests with `REQUESTED` status.

### Lab technician update test

Direct service:

```java
labService.updateLabTest(labTechnicianId, request)
```

Controller equivalent:

- `PUT /api/v1/labs/tests/update`
- Allowed role: `LAB_TECHNICIAN`

Required request field:

- `labTestId`

Optional editable fields:

- `code`
- `name`
- `unit`
- `result`
- `labTechnicianNote`

Important backend behavior:

- Service finds the test by `labTestId` and current `labTechnicianId`.
- Test must already be `IN_PROGRESS`.
- Only non-blank incoming fields are applied.
- Blank values do not clear fields.

Frontend behavior notes:

- If clearing a field is ever needed, service behavior must be extended first.

### Lab technician submit test

Direct service:

```java
labService.submitLabTest(labTechnicianId, labTestId)
```

Controller equivalent:

- `PUT /api/v1/labs/tests/{labTestId}/submit`
- Allowed role: `LAB_TECHNICIAN`

Important backend behavior:

- Test must belong to current technician.
- Test must be `IN_PROGRESS`.
- `result` must be non-blank before submit.
- Submit sets the test to `COMPLETED`.
- Submit generates a lab bill.
- If all tests in the parent request are completed, the request becomes `COMPLETED`.

## 4.5 Accountant

### Main goals

- View patient-facing appointment bills
- View patient-facing lab bills
- Search across all bills
- Confirm bill payment after insurance has finished processing

### Recommended pages

- `/accountant/dashboard/home`
- `/accountant/dashboard/appointment-bills`
- `/accountant/dashboard/lab-bills`

### Accountant appointment bills assigned to self

Direct service:

```java
billService.listAppointmentBillForAccountant(accountantId, request, pageable)
```

Controller equivalent:

- `POST /api/v1/bills/appointments/list`
- Allowed roles: `PATIENT`, `ACCOUNTANT`

Parameters:

- `accountantId`: current accountant
- `request.patientId`: optional
- `request.confirmAccountantId`: not needed, service fixes it to current accountant
- `request.status`: usually `BillStatusEnum.UNPAID`
- `request.type`: optional
- `request.appointmentId`: optional
- shared amount/date filters from `ListBillRequest`

Important backend behavior:

- Service uses the current accountant ID as the bill confirmer filter.

### Accountant search any appointment bill

Direct service:

```java
billService.searchAnyAppointmentBillForAccountant(accountantId, request, pageable)
```

Controller equivalent:

- `POST /api/v1/bills/appointments/search`
- Allowed role: `ACCOUNTANT`

Use this when:

- finance staff need a global search screen
- bill might not yet be assigned to the current accountant

### Accountant lab bills assigned to self

Direct service:

```java
billService.listLabBillForAccountant(accountantId, request, pageable)
```

Controller equivalent:

- `POST /api/v1/bills/labs/list`

Additional lab-specific parameter:

- `request.labTestId`: optional

### Accountant search any lab bill

Direct service:

```java
billService.searchAnyLabBillForAccountant(accountantId, request, pageable)
```

Controller equivalent:

- `POST /api/v1/bills/labs/search`

### Accountant confirm payment

Direct service:

```java
billService.confirmBillPayment(accountantId, billId)
```

Controller equivalent:

- `PUT /api/v1/bills/{billId}/confirm-payment`
- Allowed role: `ACCOUNTANT`

Important backend behavior:

- Works for both appointment and lab bills.
- Bill must exist.
- Cannot confirm a bill already in `PAID`.
- Cannot confirm a bill still in `VIEWING`.
- Successful confirmation sets:
  - `status = PAID`
  - `confirmAccountant = current accountant`
  - `paidOn = now`

Recommended UI:

- On accountant pages, emphasize this lifecycle:
  - `VIEWING`: insurance in progress
  - `UNPAID`: ready for payment confirmation
  - `PAID`: finished

## 4.6 Receptionist

### Main goals

- Create appointments for patients
- Search all appointments operationally
- View appointments confirmed by the receptionist
- Confirm patient attendance
- Cancel appointments
- Mark no-show after appointment end

### Recommended pages

- `/receptionist/dashboard/home`
- `/receptionist/dashboard/appointments`
- `/receptionist/dashboard/check-in`
- `/receptionist/dashboard/search`

### Receptionist create appointment

Direct service:

```java
appointmentService.createAppointmentForReceptionist(receptionistId, request)
```

Controller equivalent:

- `POST /api/v1/appointments/create`
- Allowed roles: `PATIENT`, `RECEPTIONIST`

Required request fields:

- `startTime`
- `type`
- `doctorId`
- `patientId`

Important backend behavior:

- Receptionist path still requires a patient to be chosen.
- The same booking-window, working-hour, duration, and overlap rules apply as patient self-booking.

Recommended UI:

- Use doctor picker, patient picker, and appointment-type selector.
- Validate business-friendly checks on the client before calling:
  - at least 3 days ahead
  - within working hours

### Receptionist own confirmed appointments

Direct service:

```java
appointmentService.listAppointmentForReceptionist(receptionistId, request, pageable)
```

Controller equivalent:

- `POST /api/v1/appointments/list`

Important backend behavior:

- This list filters by `confirmReceptionist.id`.
- It is best interpreted as "appointments already confirmed by this receptionist", not "all appointments created by this receptionist".

Recommended use:

- personal check-in history
- receptionist accountability log

### Receptionist global appointment search

Direct service:

```java
appointmentService.searchAnyAppointmentForReceptionist(receptionistId, request, pageable)
```

Controller equivalent:

- `POST /api/v1/appointments/search`
- Allowed role: `RECEPTIONIST`

Parameters:

- `request.patientId`: optional
- `request.doctorId`: optional
- `request.type`: optional
- `request.status`: optional
- `request.from`: optional
- `request.end`: optional

Recommended use:

- main operational appointment screen
- patient search by patient ID
- doctor schedule search
- cancellation lookup
- no-show lookup

### Receptionist confirm patient attendance

Direct service:

```java
appointmentService.confirmAppointment(receptionistId, appointmentId)
```

Controller equivalent:

- `PUT /api/v1/appointments/{appointmentId}/confirm`
- Allowed role: `RECEPTIONIST`

Important backend behavior:

- Appointment must be `CONFIRMED`.
- Confirmation is only allowed from 5 minutes before start to 15 minutes after start.
- Successful confirmation changes status to `COMPLETED`.
- This action generates the base appointment bill.
- If confirmation happens more than 5 minutes after start, it also generates a late-fee bill.

Recommended UI:

- Show check-in button only near the valid confirmation window.
- If outside the window, explain why the action is blocked.

### Receptionist cancel appointment

Direct service:

```java
appointmentService.cancelAppointment(receptionistId, RoleEnum.RECEPTIONIST, request)
```

Controller equivalent:

- `PUT /api/v1/appointments/cancel`

Parameters:

- `request.appointmentId`
- `request.cancelReason`
- `request.cancellationInitiator`

Allowed initiators for receptionist:

- `CancellationInitiatorEnum.RECEPTIONIST`
- `CancellationInitiatorEnum.RECEPTIONIST_ON_BEHALF_OF_PATIENT`

Important backend behavior:

- If receptionist cancels on behalf of patient and the cancellation is late, cancellation fee bill is generated.
- If receptionist cancels as receptionist, no patient late-cancellation fee is generated.

### Receptionist mark no-show

Direct service:

```java
appointmentService.noShowAppointment(receptionistId, appointmentId)
```

Controller equivalent:

- `PUT /api/v1/appointments/{appointmentId}/no-show`
- Allowed role: `RECEPTIONIST`

Important backend behavior:

- Can only happen after appointment end time.
- Appointment must still be `CONFIRMED`.
- Service changes status to `NO_SHOW`.
- Service always generates a cancellation fee bill.

Recommended UI:

- This should only be available from a past-due operational queue.

## 5. Request Object Cheat Sheet

### 5.1 Appointment request objects

`CreateAppointmentRequest`

- `startTime`
- `type`
- `doctorId`
- `patientId`

`ListAppointmentRequest`

- `patientId`
- `doctorId`
- `type`
- `status`
- `from`
- `end`

`CancelAppointmentRequest`

- `appointmentId`
- `cancelReason`
- `cancellationInitiator`

### 5.2 Precheck request objects

`CreatePrecheckRequest`

- `appointmentId`
- `pulse`
- `sugar`
- `temperature`
- `height`
- `weight`
- `note`

`ListPrecheckRequest`

- `appointmentId`
- `patientId`
- `doctorId`
- `nurseId`
- `status`
- `createdAtDate`

### 5.3 Lab request objects

`CreateLabRequestRequest`

- `appointmentId`
- `labTests`

Each `labTests` item:

- `code`
- `name`
- `unit`
- `doctorNote`

`ListLabRequestRequest`

- `status`
- `createdAtDate`
- `doctorId`
- `patientId`
- `appointmentId`

`ListLabTestRequest`

- `doctorId`
- `patientId`
- `status`
- `code`
- `name`
- `unit`
- `createdAtDate`

`UpdateLabTestRequest`

- `labTestId`
- `code`
- `name`
- `unit`
- `result`
- `labTechnicianNote`

### 5.4 Prescription request objects

`CreatePrescriptionRequest`

- `appointmentId`
- `startDate`
- `endDate`
- `totalRefills`
- `refillIntervalDays`
- `generalNote`
- `medicines`

Each medicine:

- `medicineName`
- `dosage`
- `frequency`
- `route`
- `instructions`
- `quantity`

`ListPrescriptionRequest`

- `patientId`
- `doctorId`
- `appointmentId`
- `status`
- `medicineName`
- `createdAtDate`

### 5.5 Patient-record-access request objects

`PatientRecordAccessRequest`

- `patientId`
- `type`

`ListPatientRecordAccessRequest`

- `patientId`
- `doctorId`
- `type`
- `status`
- `createdAtDate`

### 5.6 Bill request objects

Shared `ListBillRequest` fields:

- `minAmount`
- `maxAmount`
- `minInsuranceCoverAmount`
- `maxInsuranceCoverAmount`
- `minPatientPaymentAmount`
- `maxPatientPaymentAmount`
- `status`
- `type`
- `patientId`
- `confirmAccountantId`
- `paidOnDate`
- `createdAtDate`

`ListAppointmentBillRequest`

- all `ListBillRequest` fields
- `appointmentId`

`ListLabBillRequest`

- all `ListBillRequest` fields
- `labTestId`

## 6. Status and Lifecycle Cheat Sheet

### 6.1 Appointment status lifecycle

- Create appointment: `CONFIRMED`
- Receptionist confirms attendance: `COMPLETED`
- Patient or receptionist cancels: `CANCELLED`
- Receptionist marks missed visit after end time: `NO_SHOW`

### 6.2 Precheck status lifecycle

- Nurse creates precheck: `VALID`
- Nurse cancels during valid time window: `CANCELLED`

### 6.3 Lab request and lab test lifecycle

Lab request:

- create: `REQUESTED`
- first test claimed: `IN_PROGRESS`
- all tests completed: `COMPLETED`
- doctor cancellation: `CANCELLED`

Lab test:

- create: `REQUESTED`
- technician claim: `IN_PROGRESS`
- technician submit: `COMPLETED`
- parent cancellation: `CANCELLED`

### 6.4 Prescription lifecycle

- doctor create: `ACTIVE`
- patient uses last refill: `COMPLETED`
- doctor cancel: `CANCELLED`
- refill attempt after end date: service marks `EXPIRED`

### 6.5 Patient-record-access lifecycle

- doctor request: `PENDING`
- patient approve: `APPROVED`
- patient deny: `REJECTED`
- patient revoke: `REVOKED`
- doctor cancel pending request: `CANCELLED`

### 6.6 Bill lifecycle

- bill generated by appointment or lab completion: `VIEWING`
- insurance processing finished: `UNPAID`
- accountant confirms payment: `PAID`

Frontend implication:

- `VIEWING` means the bill exists but finance action should not be allowed yet.
- `UNPAID` is the main actionable state for accountants.
- Patient finance pages should display bill state, but payment confirmation remains an accountant workflow.

## 7. Recommended Build Order

To reduce frontend risk, implement in this order:

1. Patient upcoming appointments and review appointments
2. Receptionist appointment operations
3. Nurse precheck workflow
4. Doctor prescription workflow
5. Doctor lab-request workflow
6. Lab technician workflow
7. Accountant billing workflow
8. Patient finance, pharmacy, and access-request pages

This order follows the core operational flow:

- appointment scheduling
- appointment attendance
- precheck
- doctor action
- lab and billing
- patient follow-up
