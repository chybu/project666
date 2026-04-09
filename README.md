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

- Appointment management
  Create, list, search, confirm, and cancel appointments.
- Lab workflows
  Doctors can create lab requests, lab technicians can list/claim/update/submit tests, patients can view their own lab requests, and doctors can list shared lab requests after patient approval.
- Billing workflows
  Appointment bills and lab bills can be listed or searched, and accountants can confirm payment.
- Patient record access
  Doctors can request access to patient-owned record types. Patients can approve, deny, or revoke requests, and doctors can cancel their own requests.
- Role-based web dashboards
  Patient, doctor, nurse, and receptionist dashboard pages are available in the frontend.
- Keycloak integration
  The project supports browser login through OAuth2/OIDC and API testing through Bearer JWTs.
- Insurance integration
  Bills are published to RabbitMQ and processed asynchronously by the insurance service.

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
git clone https://github.com/chybu/project666
cd project666
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

The `frontend` module is the main runnable app and loads the backend package through dependency wiring and component scanning.

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

There are two security flows in this project.

### Frontend web app

The frontend uses OAuth2 login with Keycloak and stores a browser session after login. This is used for routes such as:

- `/patient/**`
- `/doctor/**`
- `/nurse/**`
- `/receptionist/**`

### Backend API

The backend API uses Bearer JWT authentication for:

```text
/api/**
```

That means:

- browser pages should be tested through the login flow
- API endpoints should be tested with `Authorization: Bearer <token>`

## Getting a JWT for API Testing

Import the `api-testing` client into Keycloak, copy its client secret, then request a token:

```bash
curl --location "http://localhost:9090/realms/patient-portal/protocol/openid-connect/token" \
  --header "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=password" \
  --data-urlencode "client_id=api-testing" \
  --data-urlencode "client_secret=YOUR_CLIENT_SECRET" \
  --data-urlencode "username=YOUR_USERNAME" \
  --data-urlencode "password=YOUR_PASSWORD"
```

Use the returned `access_token` like this:

```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" ...
```

## API Surface

The main backend controller groups currently are:

- `/api/v1/appointments`
  Create, list, search, confirm, and cancel appointments.
- `/api/v1/labs`
  Create/list/cancel lab requests, list shared lab requests, and list/claim/update/submit lab tests.
- `/api/v1/bills`
  List/search appointment bills, list/search lab bills, and confirm payment.
- `/api/v1/patient-record-access`
  Create/list/cancel/approve/deny/revoke patient record access requests.

For exact request and response shapes, check the DTO classes under `backend/src/main/java/com/project666/backend/domain/dto`.

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

### Confirm an appointment

```bash
curl -X PUT "http://localhost:8080/api/v1/appointments/REPLACE_APPOINTMENT_ID/confirm" \
  -H "Authorization: Bearer REPLACE_ACCESS_TOKEN"
```

Notes:

- the caller must have role `ROLE_RECEPTIONIST`
- the appointment must satisfy the confirmation rules enforced in the service layer

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

There is also a secured `labtechnician` controller, but it does not currently expose dashboard pages.

## Development Notes

- `backend` is a library module and does not have its own Spring Boot runner.
- The main runnable app is `frontend`.
- The main app connects to PostgreSQL at `jdbc:postgresql://localhost:5432/postgres`.
- The main app connects to Keycloak at `http://localhost:9090/realms/patient-portal`.
- The main app also uses RabbitMQ from local Docker services.
- `spring.jpa.hibernate.ddl-auto=update` is enabled in the frontend application properties.
- Pagination defaults are configured globally:
  - default page size: `5`
  - max page size: `20`

## Common Pitfalls

- A valid Bearer token will not work against the frontend login pages. Bearer JWTs are for `/api/**`.
- If you get `403 Forbidden` on an API request, confirm:
  - you are calling a backend API route
  - the token includes the expected `ROLE_*`
  - the endpoint role matches the caller role
- If you get business errors on appointment confirmation or cancellation, check the appointment status and time-window rules in the service layer.
- Some older notes in the repo may not match the latest controller names. Prefer the current controller and service code when in doubt.

## Testing

Run all tests with Maven.

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

These files contain additional project notes alongside the main README.
