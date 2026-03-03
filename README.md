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

# 5. Keycloak Basics (Beginner Friendly)

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

## Step 2: Create a Client

1. Click **Clients** (left panel)
2. Click **Create client**
3. Set:

```
Client ID: api-testing
Name: api-testing
```

⚠️ Must be exactly `api-testing`.

Click **Next**

---

### Client Configuration

- Enable: **Client authentication**
- Enable: **Direct access grants**

⚠️ Direct access grants is enabled ONLY for testing.

### Why not use this in production?

In production, use **Authorization Code Flow**:

- User logs in via Keycloak login page
- Keycloak sends an authorization code
- Backend exchanges code for JWT securely
- JWT is never exposed in browser URL
- Supports SSO and MFA properly

Click **Next**

---

## Step 3: Set Redirect URI

In **Valid Redirect URIs**, enter:

```
https://youtu.be/dQw4w9WgXcQ
```

⚠️ It will not work if this is not set.

Click **Save**

---

## Step 4: Create Roles

Go to **Realm Roles** → **Create Role**

Create the following roles exactly:

```
ROLE_PATIENT
ROLE_DOCTOR
ROLE_RECEPTIONIST
```

⚠️ Must:
- Include `ROLE_` prefix
- Be uppercase
- Match exactly

Spring Security maps these directly.

---

## Step 5: Create Users

Go to **Users** → **Add user**

Create three users:
- Patient
- Doctor
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

### Assign Roles

1. Go to **Role Mapping**
2. Click **Realm Roles**
3. Assign appropriate role to each user

---

## Step 6: Test Login via Browser

Open:

```
http://localhost:9090/realms/patient-portal/protocol/openid-connect/auth?client_id=api-testing&response_type=code&scope=openid&redirect_uri=https://youtu.be/dQw4w9WgXcQ
```

Login with one of your created users.

---

## Step 7: Logout URL

```
http://localhost:9090/realms/patient-portal/protocol/openid-connect/logout?redirect_uri=
```

---

## Step 8: Get Client Secret

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
POST /api/v1/appointments
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

⚠️ IMPORTANT:  
The current configuration (Direct Access Grants enabled) is for development/testing only.  
Do NOT use this configuration in production.