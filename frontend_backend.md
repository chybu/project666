# Dashboard Features Overview

---

# (1) Patient Dashboard

## Home Page
Calendar at top, Important information (upcoming appointments, prescription refills, etc.) below it.

### 1. Upcoming Appointments
Use:
backend\src\main\java\com\project666\backend\service\AppointmentService.java

Method:
listDoctorAppointment(...)

### 2. Prescription Refills
Not implemented yet

---

## Review Appointments Page
List all upcoming and past appointments. This should show the date, time, and any other important/identifying information for each appointment.

### 1. Upcoming Appointments
Use:
listDoctorAppointment(...)

### 2. Past Appointments
Use:
listDoctorAppointment(...)

### 3. Create Appointment
Use:
createAppointment(...)

### 4. Cancel Appointment
Use:
cancelAppointment(...)

---

## Finances
Bills/charges listed

### 1. Bills
Not implemented yet

---

## Messages
Able to read messages sent from Doctor/Nurse/Other staff.  
Does NOT need to be able to send messages themselves.  
Possibly only click a button to confirm or deny message contents.

Not implemented yet

---

## Notifications
Only list alerts for:
- new messages  
- appointment being scheduled / rescheduled / canceled  
- prescription being set up  
- bill due  

This should automatically redirect to the appropriate page when clicked (Finances, Messages, Pharmacy, etc.)

Not implemented yet

---

## Pharmacy
List prescription info, refill amount, pickup time, etc.

Not implemented yet

---

## Profile Settings

### Profile
- name  
- DOB  
- other important information  

Include an Edit button for relevant fields (e.g., name), but NOT DOB.

Not implemented yet

### Security
- email  
- password  
- delete account  

Allow changing relevant info like email/password.

Not implemented yet

---

# (2) Receptionist Dashboard

Note: We likely need the receptionist role because their main responsibility is to confirm patient attendance for appointments.

Home Page:
- Calendar on top  
- Staff bulletin below  

---

## Appointments
Left side of page:
- "Create Appointment" button (opens sub-UI)

Main section:
- List of "Upcoming Appointments"
- Can be sorted by:
  - name  
  - date  
  - possibly doctor  

### 1. Create Appointment
Use:
createAppointment(...)

### 2. Cancel Appointment
Use:
cancelAppointment(...)

### 3. Upcoming Appointments
Use:
listAppointment(...)

### 4. Appointments Confirmed by Receptionist
Use:
listConfirmAppointment(...)

---

## Messages
Same as Patient, but with ability to send messages.

Not implemented yet

---

## Notifications
Same as Patient

Not implemented yet

---

## Profile Settings
Same as Patient, but includes employee ID.

Not implemented yet

---

# (3) Nurse Dashboard

## Home Page
Same as Receptionist

---

## Appointments
Potentially integrate receptionist functionality here if roles are merged.

If standalone:
- Nurse can only view and sort appointments  
- Each appointment should have an "Enter Information" button  

This allows nurse to input:
- weight  
- height  
- blood pressure  
- documents  
- other check-in information  

### 1. Enter Appointment Details
Not implemented yet

---

## Messages
Same as Receptionist

Not implemented yet

---

## Notifications
Same as others

Not implemented yet

---

## Profile Settings
Same as others

Not implemented yet

---

# (4) Doctor Dashboard

## Home Page
Same as Nurse

---

## Appointments
Same as Nurse, but without "Create Appointment" functionality.

Doctor capabilities:
- Enter notes  
- Review patient documents  
- Manage appointment-related medical data  

### 1. Enter Appointment Details
Not implemented yet

---

## Messages
Same as Nurse

Not implemented yet

---

## Profile Settings
Same as others

Not implemented yet