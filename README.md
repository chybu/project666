# Patient Portal Webserver Setup Guide

This guide walks you through setting up the development environment, configuring Docker services, and setting up Keycloak authentication for API testing.

---

## 1. Prerequisites

Make sure the following software is installed:

- JDK 21
- Docker & Docker Compose
- Postman (for API testing)
- Git
---

## 2. Clone the Repository

Clone the repository:
```bash
git clone https://github.com/chybu/project666
```

---

## 3. Start the Application Stack

Launch Docker and start all services:

```bash
docker-compose up
```

---

## 4. Verify Running Services

Three services will run in Docker containers:

### 1️⃣ PostgreSQL Database
- Database engine used by the application.

### 2️⃣ Adminer (Database UI)
- URL: http://localhost:8888  
- System: PostgreSQL  
- Username: `admin`  
- Password: `password`

Adminer allows you to interact with the database via a browser.

### 3️⃣ Keycloak (User Management & Authentication)
- URL: http://localhost:9090  
- Username: `admin`  
- Password: `password`

Keycloak handles authentication, authorization, roles, and token management.

---

# 5. Keycloak Basics

### What is Keycloak?

Keycloak is an open-source Identity and Access Management (IAM) system.  
It handles:

- User authentication (login)
- Role-based authorization
- Token (JWT) generation
- Single Sign-On (SSO)
- Multi-Factor Authentication (MFA)

---

### What is a Realm?

A **Realm** is an isolated space inside Keycloak that manages:

- Users
- Roles
- Clients
- Authentication settings

Keycloak has a default `master` realm used to manage Keycloak itself.

For this project, we will create a separate realm:
```
patient-portal
```

---

### What is a Client?

A **Client** represents an application that uses Keycloak for authentication.

Examples:
- Backend API
- Frontend web app
- Mobile app

Each client has:
- A Client ID
- Authentication settings
- Redirect URLs
- A Client Secret (for confidential clients)

---

# 6. Keycloak Configuration (Step-by-Step)

## Step 1: Create a Realm

1. Go to **Manage Realms** (top-left corner)
2. Click **Create Realm**
3. Enter name:

```
patient-portal
```

⚠️ The name must be exactly `patient-portal` because backend API endpoints depend on it.

Click **Create**.

To switch realms:
- Click the realm name in the top-left dropdown.
- Ensure it shows `patient-portal`.

---

## Step 2: Config `patient-portal` realm

1. Switch to `patient-portal` realm
2. Click **Realm settings** (left panel)
3. Click **Login** tab
4. Enable `User registration`
5. Click **Events** tab
6. On the Event listeners, add `patient-role-listener`
7. Click `Save`

---

## Step 3: Create API Testing Client

1. Click **Clients** (left panel)
2. Click **Import client**
3. Click `Browse` and Select the json file inside this repo:`keycloak_clients/api-testing.json`

⚠️ In `api-testing` client, `Direct access grants` is enabled ONLY for testing.

### Why not use this in production?

In production, use **Authorization Code Flow**:

- User logs in via Keycloak login page
- Keycloak sends an authorization code
- Backend exchanges code for JWT securely
- JWT is never exposed in browser URL
- Supports SSO and MFA properly

---

## Step 4: Create Frontend Client
1. Click **Clients** (left panel)
2. Click **Import client**
3. Click `Browse` and Select the json file inside this repo: `keycloak_clients/frontend.json`

---

## Step 5.1: Create Roles

Go to **Realm Roles** → **Create Role**

Create the following roles exactly:

```
ROLE_PATIENT
ROLE_DOCTOR
ROLE_NURSE
ROLE_RECEPTIONIST
```

⚠️ Must:
- Include `ROLE_` prefix
- Be uppercase
- Match exactly

Spring Security maps these directly.

---


## Step 6: Create Users

Go to **Users** → **Add user**

Create four users (Test User Information Provided Below "Set Password" Section):
- Patient
- Doctor
- Nurse
- Receptionist

Fill:
- Username
- Email
- First Name
- Last Name

Click **Create**

---

### Set Password

1. Go to **Credentials**
2. Click **Set Password**
3. Enter password
4. Turn OFF "Temporary"
5. Save

---

### Test User Information
Users:

* Patients

Username - testPatient
Email - testPatient501@gmail.com
First Name - Ryan
Last Name - Brewster
password - 123TestPatient!


* Doctors

Username - testDoctor
Email - TestDoctor502@gmail.com
First Name - Greggory
Last Name - House
password - 123TestDoctor!

* Nurses

Username - testNurse
Email - TestNurse501@gmail.com
First Name - Carmen
Last Name - idk
password - 123TestNurse!

* Receptionists

Username - testRecep
Email - testRecep501@gmail.com
First Name - Sam
Last Name - Something
password - 123TestRecep!

---

### Assign Roles

1. Go to **Role Mapping**
2. Click **Realm Roles**
3. Assign appropriate role to each user

---

## Step 7: Get Client Secret

1. Go to **Clients**
2. Select `api-testing`
3. Go to **Credentials**
4. Copy the **Client Secret**

---

# 7. Get JWT Token Using Postman

Import this curl into Postman (replace placeholders):

```bash
curl --location 'http://localhost:9090/realms/patient-portal/protocol/openid-connect/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode grant_type=password \
--data-urlencode client_id=api-testing \
--data-urlencode client_secret=YOUR_CLIENT_SECRET \
--data-urlencode username=YOUR_USERNAME \
--data-urlencode password=YOUR_PASSWORD
```

Click **Send**

You will receive:

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI..."
}
```

Copy the `access_token`.

---

## Decode JWT

Go to:

https://jwt.io

Paste the token to decode it.

This JWT contains:
- User identity
- Assigned roles
- Expiration time
- Issuer

---

# 8. Available API Endpoints

Base URL:
```
http://localhost:8080
```

---

### Appointment APIs

### Create Appointment
```
POST /api/v1/appointments/create
```
Accessible by:
- ROLE_PATIENT
- ROLE_RECEPTIONIST

---

### Confirm Appointment
```
PUT /api/v1/appointments/{appointmentId}/confirm
```
Accessible by:
- ROLE_RECEPTIONIST

---

### Cancel Appointment
```
PUT /api/v1/appointments/cancel
```
Accessible by:
- ROLE_PATIENT
- ROLE_RECEPTIONIST

---

### Search Appointments
```
GET /api/v1/appointments/search
```
Accessible by:
- ROLE_RECEPTIONIST

---

### List Appointments
```
GET /api/v1/appointments
```

Behavior depends on role:

- Patient → List upcoming appointments
- Doctor → List appointments assigned to you
- Receptionist → List confirmed appointments

All endpoints support filtering using appointment parameters.

---

# 9. Run The Website

Run:
```bash
./mvnw clean install
```
```bash
./mvnw -f frontend/pom.xml spring-boot:run
```
