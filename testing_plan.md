# Testing Plan

## Workflow Assignment

| Tester | Workflow |
| --- | --- |
| Trung | Billing |
| Ethan | Precheck and Patient Access and Records |
| Thomas | Appointments, Clinical, and Labs |

## Trung

### Billing

Check:

- Accountant can open appointment bills
- Accountant can open lab bills
- Bill filters work for date range, amount range, patient, and status
- Bills created from completed workflows are visible
- Accountant can confirm payment for appointment bills
- Accountant can confirm payment for lab bills
- Paid status updates correctly after confirmation
- Duplicate or invalid payment confirmation is handled safely

## Ethan

### Precheck

Check:

- Receptionist check-in creates the right starting point for nurse work (nurse can only submit precheck after recep confirm)
- Nurse can open today's appointment list
- Nurse can filter by patient and doctor
- Nurse can submit a valid precheck
- Submitted precheck shows up in the nurse views
- Nurse can cancel a precheck
- Cancelled precheck no longer behaves like an active precheck
- Precheck history loads correctly for the patient
- Precheck list filters work for patient, date range, and status
- Invalid precheck submission shows a safe error

### Patient Access and Records

Check:

- Patient login works correctly
- Patient home dashboard loads correctly
- Appointment calendar and appointment lists load correctly
- Patient can book an appointment
- Patient can cancel an eligible appointment
- Finances page shows the patient's bills
- Pharmacy page shows prescriptions
- Patient can consume a refill when eligible
- Records page shows patient-owned records
- Access requests page shows doctor requests
- Patient can approve an access request
- Patient can deny an access request
- Patient can revoke an approved access request
- Profile and security pages load correctly
- Patient delete-account workflow works correctly
- Patient cannot access dashboards for other roles

## Thomas

### Appointments

Check:

- Receptionist home dashboard loads correctly
- Appointment search works with patient, doctor, status, and date filters
- Confirmed appointments view works
- Receptionist can create a valid appointment
- Invalid appointment creation is blocked
- Receptionist can check in an eligible appointment
- Receptionist can cancel an eligible appointment
- Receptionist can mark an eligible appointment as no-show
- Appointment status updates correctly after each action

### Clinical

Check:

- Doctor home dashboard loads correctly
- Doctor can review upcoming and completed appointments
- Doctor appointment filters work
- Doctor can open prechecks
- Doctor can create a prescription
- Doctor can view prescription details
- Doctor can cancel an eligible prescription
- Doctor can create a lab request
- Doctor can view lab details
- Doctor can cancel an eligible lab request
- Doctor can create a patient record access request
- Doctor can cancel a pending access request
- Shared records become visible after patient approval
- Shared records are blocked again after patient denial or revocation

### Labs

Check:

- Lab technician can open the lab request queue
- Lab request filters work
- Lab technician can claim an available lab test
- In-progress lab tests page loads correctly
- Lab test filters work for patient, doctor, status, code, name, unit, and date range
- Lab technician can update a claimed lab test
- Lab technician can submit a completed lab test
- Completed lab tests no longer behave like in-progress work
- Invalid update or submit flows are handled safely
