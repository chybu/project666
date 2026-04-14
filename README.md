# Patient Portal

Patient Portal is a multi-module Spring Boot project for hospital workflow management. It includes:

- a server-rendered web app with role-based dashboards
- a secured REST API for appointments, labs, billing, and patient record access
- a separate insurance worker that reacts to billing events over RabbitMQ

## Architecture

The repository is split into three Maven modules:

- `frontend`
  The main runnable web application. It contains the Spring Boot entry point, Thymeleaf pages, OAuth2 login, and depends on the `backend` module.
- `backend`
  Shared domain logic, JPA repositories, services, mappers, API controllers, and Bearer-token security for `/api/**`.
- `insurance`
  A separate Spring Boot app that listens for billing events on RabbitMQ and publishes insurance-completion messages back to the main app.

## Current Features

- Appointment workflow
  Patients and receptionists can create appointments, role-scoped appointment lists are available for patients, doctors, receptionists, and nurses, receptionists can search globally, confirm check-in, cancel appointments, and mark no-shows. Booking windows, working hours, overlap protection, and late/cancellation fee rules are enforced in the service layer.
- Precheck workflow
  Nurses can create and cancel prechecks for attended appointments, and patients, doctors, and nurses can list relevant prechecks. Doctors can also view shared prechecks after patient approval.
- Prescription workflow
  Doctors can create and cancel prescriptions, patients can list their own prescriptions and consume eligible refills, and doctors can view shared prescriptions after patient approval.
- Lab workflow
  Doctors can create and cancel lab requests, lab technicians can list request queues and assigned tests, claim tests, update results, and submit completed tests. Patients can view their own lab requests, and doctors can view shared lab requests after patient approval.
- Billing and insurance workflow
  Appointment bills and lab bills are generated automatically from appointment/lab events, patients and accountants can list bills, accountants can search and confirm payments, and the insurance worker processes bill events asynchronously through RabbitMQ.
- Patient record access workflow
  Doctors can request access to patient-owned `PRECHECK`, `LAB_REQUEST`, and `PRESCRIPTION` records. Patients can approve, deny, or revoke requests, and doctors can cancel their own pending requests.
- Role-based server-rendered frontend
  The frontend includes dashboard routes and profile/account pages for patient, doctor, nurse, receptionist, lab technician, and accountant roles. Patient appointment pages are already wired to the backend service layer, and a detailed SSR integration design is documented in `frontend.md`.
- Security and identity integration
  The project supports browser login through OAuth2/OIDC with Keycloak, role-based routing in the web app, and Bearer JWT authentication for the REST API.

## Roles

The codebase currently recognizes these application roles:

- `ROLE_PATIENT`
- `ROLE_DOCTOR`
- `ROLE_RECEPTIONIST`
- `ROLE_NURSE`
- `ROLE_LAB_TECHNICIAN`
- `ROLE_ACCOUNTANT`

## Tech Stack

- Java 21
- Spring Boot 4.0.2
- Spring Security
- Spring Data JPA
- Thymeleaf
- PostgreSQL
- Keycloak
- RabbitMQ
- MapStruct
- Lombok

## Prerequisites

- JDK 21
- Docker Desktop or Docker Engine with Compose
- Maven Wrapper support
- Git

## Local Services

`docker-compose.yml` starts the supporting infrastructure:

- PostgreSQL: `localhost:5432`
- Adminer: `http://localhost:8888`
- Keycloak: `http://localhost:9090`
- RabbitMQ AMQP: `localhost:5672`
- RabbitMQ Management UI: `http://localhost:15672`

Default local credentials from the repo:

- PostgreSQL admin user: `admin`
- PostgreSQL admin password: `password`
- Adminer login: use PostgreSQL with the same credentials above
- Keycloak admin user: `admin`
- Keycloak admin password: `password`
- RabbitMQ user: `insurance`
- RabbitMQ password: `password`

## Quick Start

### 1. Clone the repository

```bash
git clone https://github.com/chybu/Duplex-Patient-Portal
cd Duplex-Patient-Portal
```

### 2. Start infrastructure

```bash
docker compose up -d
```

### 3. Configure Keycloak

Open `http://localhost:9090` and log in as `admin` / `password`.

Create the `patient-portal` realm and import these clients:

- `keycloak_clients/api-testing.json`
- `keycloak_clients/frontend.json`
- `keycloak_clients/backend-service.json`
- Go to backend-service clients, `Service account roles` tab, click `Assign role`, click `Client roles`, add `manage-users` and `view-users`

The project also includes a custom Keycloak provider in `providers/`, so keep that folder mounted when running Keycloak through Docker.

### 4. Create realm roles

Create these realm roles:

```text
ROLE_PATIENT
ROLE_DOCTOR
ROLE_RECEPTIONIST
ROLE_NURSE
ROLE_LAB_TECHNICIAN
ROLE_ACCOUNTANT
```

### 5. Create test users

You can create any local users you want, but these accounts are handy for local testing:

```text
Patient
username: testPatient
password: 123TestPatient!
email: testPatient501@gmail.com

Doctor
username: testDoctor
password: 123TestDoctor!
email: TestDoctor502@gmail.com

Nurse
username: testNurse
password: 123TestNurse!
email: TestNurse501@gmail.com

Receptionist
username: testRecep
password: 123TestRecep!
email: testRecep501@gmail.com

Accountant
Username: testAcc
password: 123TestAcc!
Email: testAccWork501@gmail.com

Lab Technician
Username: testLab
password: 123TestLab!
Email: testLabTech501@gmail.com
```

Assign each user the matching realm role.

### 6. Run the insurance service

Windows:

```powershell
.\mvnw.cmd -f insurance\pom.xml clean spring-boot:run
```

macOS/Linux:

```bash
./mvnw -f insurance/pom.xml clean spring-boot:run
```

### 7. Run the main application

The `frontend` module is the main runnable app and loads the backend package through dependency wiring and component scanning.

Windows:
```powershell
.\mvnw.cmd -f backend\pom.xml clean install
```
```powershell
.\mvnw.cmd -f frontend\pom.xml spring-boot:run
```

macOS/Linux:
```bash
./mvnw -f backend/pom.xml clean install
```
```bash
./mvnw -f frontend/pom.xml spring-boot:run
```

## Application URLs

- Main app: `http://localhost:8080`
- Landing page: `http://localhost:8080/`
- Keycloak: `http://localhost:9090`
- Adminer: `http://localhost:8888`
- RabbitMQ management: `http://localhost:15672`
