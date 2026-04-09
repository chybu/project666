# Patient Portal

Patient Portal is a multi-module Spring Boot project for hospital workflow management. It combines:

- a server-rendered web app for role-based dashboards
- a secured REST API for appointments, labs, billing, and patient record access
- a separate insurance worker that reacts to billing events over RabbitMQ

## Architecture

The repository is split into three Maven modules:

- `frontend`
  This is the runnable web application. It contains the `DemoApplication` entry point, Thymeleaf pages, OAuth2 login, and pulls in the `backend` module as a dependency.
- `backend`
  This contains the domain model, JPA repositories, services, mappers, security for Bearer-token API access, and REST controllers under `/api/v1/**`.
- `insurance`
  This is a separate Spring Boot app that listens for billing events on RabbitMQ and publishes insurance-completion messages back to the main app.

## Current Features

- Appointment management
  Create, list, search, confirm, cancel, and mark no-show appointments.
- Lab workflows
  Doctors create lab requests, lab technicians claim/update/submit lab tests, patients can view their own lab requests, and doctors can view shared lab requests after patient approval.
- Billing workflows
  Appointment and lab bills can be listed, searched, and confirmed as paid by accountants.
- Patient record access
  Doctors can request access to patient-owned record types such as lab requests and prescriptions. Patients can approve, deny, revoke, or doctors can cancel requests.
- Role-based web dashboards
  Patient, doctor, nurse, and receptionist dashboard pages are available in the frontend.
- Keycloak integration
  The app supports browser login through OAuth2/OIDC and API testing through Bearer JWTs.
- Insurance integration
  Bills are pushed to RabbitMQ and processed asynchronously by the insurance service.

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
- Spring Boot 4
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
git clone https://github.com/chybu/project666
cd project666
```

### 2. Start infrastructure

```bash
docker compose up -d
```

### 3. Configure Keycloak

Open `http://localhost:9090` and log in as `admin` / `password`.

Set up the `patient-portal` realm and clients using the JSON files in:

- `keycloak_clients/api-testing.json`
- `keycloak_clients/frontend.json`

The project also includes a custom Keycloak provider in `providers/`, so keep that folder mounted through Docker when running Keycloak.

### 4. Create realm roles

Create these realm roles exactly:

```text
ROLE_PATIENT
ROLE_DOCTOR
ROLE_RECEPTIONIST
ROLE_NURSE
ROLE_LAB_TECHNICIAN
ROLE_ACCOUNTANT
```

### 5. Create test users

You can create your own users, but these test accounts match the existing docs and are useful for local testing:

```text
Patient
username: testPatient
password: 123TestPatient!

Doctor
username: testDoctor
password: 123TestDoctor!

Nurse
username: testNurse
password: 123TestNurse!

Receptionist
username: testRecep
password: 123TestRecep!
```

Assign each user the matching realm role.

### 6. Run the insurance service

Windows:

```powershell
.\mvnw.cmd -f insurance\pom.xml spring-boot:run
```

macOS/Linux:

```bash
./mvnw -f insurance/pom.xml spring-boot:run
```

### 7. Run the main application

The `frontend` module is the runnable app and it loads the backend package through component scanning.

Windows:

```powershell
.\mvnw.cmd -f frontend\pom.xml spring-boot:run
```

macOS/Linux:

```bash
./mvnw -f frontend/pom.xml spring-boot:run
```

## Application URLs

- Main app: `http://localhost:8080`
- Landing page: `http://localhost:8080/`
- Keycloak: `http://localhost:9090`
- Adminer: `http://localhost:8888`
- RabbitMQ management: `http://localhost:15672`

## Authentication Model

There are two different security flows in this project.

### Frontend web app

The frontend uses OAuth2 login with Keycloak and stores a browser session after login. This is used for pages like:

- `/patient/**`
- `/doctor/**`
- `/nurse/**`
- `/receptionist/**`

### Backend API

The backend API uses Bearer JWT authentication for routes under:

```text
/api/**
```

This means:

- browser pages should be tested through the login flow
- API endpoints should be tested with `Authorization: Bearer <token>`

## Getting a JWT for API Testing

Import the `api-testing` client into Keycloak and copy its client secret. Then request a token:

```bash
curl --location "http://localhost:9090/realms/patient-portal/protocol/openid-connect/token" \
  --header "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=password" \
  --data-urlencode "client_id=api-testing" \
  --data-urlencode "client_secret=YOUR_CLIENT_SECRET" \
  --data-urlencode "username=YOUR_USERNAME" \
  --data-urlencode "password=YOUR_PASSWORD"
```

Use the returned `access_token` as:

```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" ...
```

## API Surface

The main backend controller groups currently are:

- `/api/v1/appointments`
  Appointment create/list/search/confirm/cancel/no-show flows.
- `/api/v1/labs`
  Lab request creation, shared lab viewing, lab test list/claim/update/submit.
- `/api/v1/bills`
  Appointment bill listing/search, lab bill listing/search, payment confirmation.
- `/api/v1/patient-record-access`
  Access request create/list/cancel/approve/deny/revoke.

For the exact request and response shapes, check the DTO classes in `backend/src/main/java/com/project666/backend/domain/dto`.

## Useful Example Requests

### Get a receptionist token

```bash
curl --location "http://localhost:9090/realms/patient-portal/protocol/openid-connect/token" \
  --header "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=password" \
  --data-urlencode "client_id=api-testing" \
  --data-urlencode "client_secret=YOUR_CLIENT_SECRET" \
  --data-urlencode "username=testRecep" \
  --data-urlencode "password=123TestRecep!"
```

### Mark an appointment as no-show

```bash
curl -X PUT "http://localhost:8080/api/v1/appointments/REPLACE_APPOINTMENT_ID/no-show" \
  -H "Authorization: Bearer REPLACE_ACCESS_TOKEN"
```

Notes:

- the caller must have role `ROLE_NURSE` or `ROLE_RECEPTIONIST`
- the appointment must currently be in `CONFIRMED` status

## Frontend Pages

The web app currently includes dashboard routes for:

- Patient
  Home, review appointments, finances, notifications, pharmacy, profile, security
- Doctor
  Home, appointments, notifications, profile
- Nurse
  Home, appointments, notifications, profile
- Receptionist
  Home, appointments, notifications, profile

The lab technician route exists in the frontend security/controller layer, but the dashboard pages are not yet fleshed out in the same way as the other roles.

## Development Notes

- `backend` is a library module and does not have its own Spring Boot runner.
- The main runnable app is `frontend`.
- The main app connects to PostgreSQL at `jdbc:postgresql://localhost:5432/postgres`.
- The main app also uses RabbitMQ and Keycloak directly from local Docker services.
- `spring.jpa.hibernate.ddl-auto=update` is enabled in the frontend application properties.
- Pagination defaults are configured globally:
  - default page size: `5`
  - max page size: `20`

## Common Pitfalls

- A valid Bearer token will not work against the frontend login pages. Bearer JWTs are for `/api/**`.
- If you get `403 Forbidden` on an API request, first confirm:
  - you are calling the backend API route
  - the token includes the expected `ROLE_*`
  - the endpoint role matches the caller role
- If you get business errors on appointment confirmation or no-show, check the appointment status and allowed time window rules in the service layer.
- Some older notes in the repo may not match the latest controller names or role list. Prefer the controller and service code when in doubt.

## Testing

Run the module tests with Maven.

Windows:

```powershell
.\mvnw.cmd test
```

macOS/Linux:

```bash
./mvnw test
```

You can also run tests per module:

```bash
./mvnw -f backend/pom.xml test
./mvnw -f frontend/pom.xml test
./mvnw -f insurance/pom.xml test
```

## Related Notes in the Repo

- `frontend_backend.md`
- `workflow.md`
- `lab and bill.md`
- `Thomas.md`

These files contain project-specific implementation notes that complement the README.
