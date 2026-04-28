-- Test data seed for 6 stakeholders (PostgreSQL)
-- Source users are based on export/patient-portal-realm.json
-- Stakeholders: PATIENT, RECEPTIONIST, DOCTOR, NURSE, LAB_TECHNICIAN, ACCOUNTANT
--
-- Dynamic-time fixtures:
-- - Appointment a100...0001 starts in ~4 minutes (confirm-appointment happy path).
-- - Appointment a100...0003 is currently in progress and COMPLETED (precheck can be created now).
-- - Appointment a100...0002 started ~30 minutes ago and is still CONFIRMED (no-show flow test).

BEGIN;

CREATE TEMP TABLE _seed_clock (
    now_ts timestamp(6) NOT NULL
) ON COMMIT DROP;

INSERT INTO _seed_clock (now_ts) VALUES (LOCALTIMESTAMP);

CREATE TABLE IF NOT EXISTS users (
    id uuid PRIMARY KEY,
    email varchar(255) NOT NULL,
    name varchar(255) NOT NULL,
    first_name varchar(255) NOT NULL,
    last_name varchar(255) NOT NULL,
    role varchar(255) NOT NULL,
    deleted boolean NOT NULL DEFAULT false,
    created_at timestamp(6) NOT NULL,
    last_updated timestamp(6) NOT NULL,
    CONSTRAINT users_role_check CHECK (
        role IN ('PATIENT', 'RECEPTIONIST', 'DOCTOR', 'NURSE', 'LAB_TECHNICIAN', 'ACCOUNTANT')
    )
);

CREATE TABLE IF NOT EXISTS appointments (
    id uuid PRIMARY KEY,
    start_time timestamp(6) NOT NULL,
    end_time timestamp(6) NOT NULL,
    type varchar(255) NOT NULL,
    status varchar(255) NOT NULL,
    created_at timestamp(6) NOT NULL,
    creator_id uuid NOT NULL REFERENCES users(id),
    last_updated timestamp(6) NOT NULL,
    canceller_id uuid REFERENCES users(id),
    cancellation_initiator varchar(255),
    cancel_reason varchar(255),
    cancelled_at timestamp(6),
    confirm_receptionist_id uuid REFERENCES users(id),
    confirmed_at timestamp(6),
    doctor_id uuid NOT NULL REFERENCES users(id),
    patient_id uuid NOT NULL REFERENCES users(id),
    version bigint DEFAULT 0,
    CONSTRAINT appointments_type_check CHECK (
        type IN ('QUICK_CHECK', 'MID_CHECK', 'LONG_CHECK')
    ),
    CONSTRAINT appointments_status_check CHECK (
        status IN ('CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')
    ),
    CONSTRAINT appointments_cancellation_initiator_check CHECK (
        cancellation_initiator IN ('PATIENT', 'RECEPTIONIST', 'RECEPTIONIST_ON_BEHALF_OF_PATIENT')
    )
);

CREATE TABLE IF NOT EXISTS patient_record_access (
    id uuid PRIMARY KEY,
    record_type varchar(255) NOT NULL,
    patient_id uuid NOT NULL REFERENCES users(id),
    doctor_id uuid NOT NULL REFERENCES users(id),
    status varchar(255) NOT NULL,
    created_at timestamp(6) NOT NULL,
    last_updated timestamp(6) NOT NULL,
    CONSTRAINT patient_record_access_record_type_check CHECK (
        record_type IN ('LAB_REQUEST', 'PRESCRIPTION', 'PRECHECK')
    ),
    CONSTRAINT patient_record_access_status_check CHECK (
        status IN ('PENDING', 'APPROVED', 'REJECTED', 'REVOKED', 'CANCELLED')
    )
);

CREATE TABLE IF NOT EXISTS prechecks (
    id uuid PRIMARY KEY,
    appointment_id uuid NOT NULL REFERENCES appointments(id),
    patient_id uuid NOT NULL REFERENCES users(id),
    doctor_id uuid NOT NULL REFERENCES users(id),
    nurse_id uuid NOT NULL REFERENCES users(id),
    status varchar(255) NOT NULL,
    pulse integer NOT NULL,
    sugar double precision NOT NULL,
    temperature double precision NOT NULL,
    height double precision NOT NULL,
    weight double precision NOT NULL,
    note varchar(255),
    created_at timestamp(6) NOT NULL,
    last_updated timestamp(6) NOT NULL,
    version bigint DEFAULT 0,
    CONSTRAINT prechecks_status_check CHECK (
        status IN ('VALID', 'CANCELLED')
    )
);

CREATE TABLE IF NOT EXISTS lab_requests (
    id uuid PRIMARY KEY,
    status varchar(255) NOT NULL,
    created_at timestamp(6) NOT NULL,
    last_updated timestamp(6) NOT NULL,
    version bigint DEFAULT 0,
    doctor_id uuid NOT NULL REFERENCES users(id),
    patient_id uuid NOT NULL REFERENCES users(id),
    appointment_id uuid NOT NULL REFERENCES appointments(id),
    CONSTRAINT lab_requests_status_check CHECK (
        status IN ('REQUESTED', 'IN_PROGRESS', 'CANCELLED', 'COMPLETED')
    )
);

CREATE TABLE IF NOT EXISTS lab_tests (
    id uuid PRIMARY KEY,
    status varchar(255) NOT NULL,
    code varchar(255),
    name varchar(255) NOT NULL,
    result varchar(255),
    unit varchar(255),
    lab_technician_note varchar(255),
    doctor_note varchar(255),
    created_at timestamp(6) NOT NULL,
    last_updated timestamp(6) NOT NULL,
    lab_technician_id uuid REFERENCES users(id),
    lab_request_id uuid NOT NULL REFERENCES lab_requests(id),
    patient_id uuid REFERENCES users(id),
    version bigint DEFAULT 0,
    CONSTRAINT lab_tests_status_check CHECK (
        status IN ('REQUESTED', 'IN_PROGRESS', 'CANCELLED', 'COMPLETED')
    )
);

CREATE TABLE IF NOT EXISTS prescriptions (
    id uuid PRIMARY KEY,
    status varchar(255) NOT NULL,
    doctor_id uuid NOT NULL REFERENCES users(id),
    patient_id uuid NOT NULL REFERENCES users(id),
    appointment_id uuid NOT NULL REFERENCES appointments(id),
    start_date date NOT NULL,
    end_date date NOT NULL,
    total_refills integer NOT NULL,
    remaining_refills integer NOT NULL,
    refill_interval_days integer NOT NULL,
    next_eligible_refill_at timestamp(6) NOT NULL,
    last_consumed_at timestamp(6),
    general_note varchar(255),
    created_at timestamp(6) NOT NULL,
    last_updated timestamp(6) NOT NULL,
    version bigint DEFAULT 0,
    CONSTRAINT prescriptions_status_check CHECK (
        status IN ('ACTIVE', 'COMPLETED', 'CANCELLED', 'EXPIRED')
    )
);

CREATE TABLE IF NOT EXISTS prescription_medicines (
    id uuid PRIMARY KEY,
    prescription_id uuid NOT NULL REFERENCES prescriptions(id),
    medicine_name varchar(255) NOT NULL,
    dosage varchar(255),
    frequency varchar(255),
    route varchar(255),
    instructions varchar(255),
    quantity varchar(255),
    created_at timestamp(6) NOT NULL,
    last_updated timestamp(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS appointment_bills (
    id uuid PRIMARY KEY,
    amount numeric(38,2) NOT NULL,
    insurance_cover_amount numeric(38,2),
    patient_payment_amount numeric(38,2),
    status varchar(255) NOT NULL,
    type varchar(255) NOT NULL,
    patient_id uuid NOT NULL REFERENCES users(id),
    confirm_accountant_id uuid REFERENCES users(id),
    paid_on timestamp(6),
    created_at timestamp(6) NOT NULL,
    last_updated timestamp(6) NOT NULL,
    version bigint DEFAULT 0,
    appointment_id uuid NOT NULL REFERENCES appointments(id),
    CONSTRAINT appointment_bills_status_check CHECK (
        status IN ('VIEWING', 'PAID', 'UNPAID')
    ),
    CONSTRAINT appointment_bills_type_check CHECK (
        type IN (
            'QUICK_CHECK_FEE',
            'MID_CHECK_FEE',
            'LONG_CHECK_FEE',
            'QUICK_CHECK_CANCELLATION_FEE',
            'MID_CHECK_CANCELLATION_FEE',
            'LONG_CHECK_CANCELLATION_FEE',
            'LATE_FEE',
            'LAB_TEST_FEE'
        )
    )
);

CREATE TABLE IF NOT EXISTS lab_bills (
    id uuid PRIMARY KEY,
    amount numeric(38,2) NOT NULL,
    insurance_cover_amount numeric(38,2),
    patient_payment_amount numeric(38,2),
    status varchar(255) NOT NULL,
    type varchar(255) NOT NULL,
    patient_id uuid NOT NULL REFERENCES users(id),
    confirm_accountant_id uuid REFERENCES users(id),
    paid_on timestamp(6),
    created_at timestamp(6) NOT NULL,
    last_updated timestamp(6) NOT NULL,
    version bigint DEFAULT 0,
    lab_test_id uuid NOT NULL REFERENCES lab_tests(id),
    CONSTRAINT lab_bills_status_check CHECK (
        status IN ('VIEWING', 'PAID', 'UNPAID')
    ),
    CONSTRAINT lab_bills_type_check CHECK (
        type IN (
            'QUICK_CHECK_FEE',
            'MID_CHECK_FEE',
            'LONG_CHECK_FEE',
            'QUICK_CHECK_CANCELLATION_FEE',
            'MID_CHECK_CANCELLATION_FEE',
            'LONG_CHECK_CANCELLATION_FEE',
            'LATE_FEE',
            'LAB_TEST_FEE'
        )
    )
);

WITH clock AS (
    SELECT now_ts FROM _seed_clock
)
,
users_seed (id, email, name, first_name, last_name, role) AS (
    VALUES
        ('2c354b57-9111-4806-a365-10ba3337d2fc'::uuid, 'din.jarin@hospital.com', 'acct.jarin', 'din', 'jarin', 'ACCOUNTANT'),
        ('863dc859-a714-4bee-91de-8cbe72c5aa08'::uuid, 'doc1@hospital.com', 'doc1', 'larry', 'james', 'DOCTOR'),
        ('e7dc4b8d-ec5e-4a97-bdbc-dfeb158c7bd6'::uuid, 'strange@marvel.com', 'doctor_strange', 'Strange', 'Potter', 'DOCTOR'),
        ('9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid, 'meredith.grey@hospital.com', 'dr.grey', 'Meredith', 'Grey', 'DOCTOR'),
        ('fdc76d3e-0e13-40ca-9954-164c70ed180f'::uuid, 'gregory.house@hospital.com', 'dr.house', 'Gregory', 'House', 'DOCTOR'),
        ('43c692d1-ed95-44a9-a56a-37d91a0902c4'::uuid, 'thebestreceptionist@hospital.com', 'i_am_a_receptionist', 'Harry', 'Lee', 'RECEPTIONIST'),
        ('47f6cbf0-1bb8-4600-9cc7-3adcc215c31c'::uuid, 'jiri@ufc.com', 'jiri.ufc', 'jiri', 'prochazka', 'PATIENT'),
        ('010e2010-95c7-460d-a39a-74c540158661'::uuid, 'jespink@yahoo.com', 'j.pinkman', 'Jesse', 'Pinkman', 'PATIENT'),
        ('539b4c43-be0f-448f-b1ee-d4ed310491ca'::uuid, 'jschlatt@gmail.com', 'jschlatt', 'jebediah', 'schlatt', 'PATIENT'),
        ('1f834870-ee09-4c54-89c6-14669f83f2c7'::uuid, 'labtech@gmail.com', 'labtech', 'lab', 'tech', 'LAB_TECHNICIAN'),
        ('ac8337d4-e4f9-48cb-8822-17d827d07f71'::uuid, 'nur1@hospital.com', 'nur1', 'nurse', '1', 'NURSE'),
        ('00c3523b-1af8-4f8d-95fe-8af3e7829763'::uuid, 'nurse@gmail.com', 'nurse', 'nurse', 'x', 'NURSE'),
        ('6f84464f-73f4-48a1-bbbc-cb668d4ebc37'::uuid, 'florence.jared@hospital.com', 'nurse.jared', 'florence', 'jared', 'NURSE'),
        ('773a5786-0ac7-42bc-bf48-2db465b5e2e5'::uuid, 'larry.pest@hospital.com', 'nurse.pest', 'larry', 'pest', 'NURSE'),
        ('3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid, 'pat1@gmail.com', 'pat1', 'john', 'marshall', 'PATIENT'),
        ('13f247e0-ca31-4240-a9c8-3207e6ec87e7'::uuid, 'recep1@hospital.com', 'recep1', 'henry', 'gopher', 'RECEPTIONIST'),
        ('1ce33e60-07ec-4ffa-8095-23d711ecf1a3'::uuid, 'lance.shilly@hospital.com', 'recep.shilly', 'lance', 'shilly', 'RECEPTIONIST'),
        ('57f2a376-447f-4648-83aa-c70f484d37fa'::uuid, 'sheldon.cooper@caltech.edu', 's.cooper', 'sheldon', 'cooper', 'PATIENT'),
        ('6fd481d3-a6b7-4371-af0f-a82be85b2848'::uuid, 'c.kent@dailyplanet.com', 'superman', 'Clark', 'Kent', 'PATIENT'),
        ('be5ed14f-8685-461b-bf5e-73b5d443fa1f'::uuid, 't@gmail.com', 'test', 'Tronv', 'Cena', 'PATIENT')
)
INSERT INTO users (
    id, email, name, first_name, last_name, role, deleted, created_at, last_updated
)
SELECT
    u.id,
    u.email,
    u.name,
    u.first_name,
    u.last_name,
    u.role,
    false,
    c.now_ts - interval '90 days',
    c.now_ts
FROM users_seed u
CROSS JOIN clock c
ON CONFLICT (id) DO UPDATE
SET
    email = EXCLUDED.email,
    name = EXCLUDED.name,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    role = EXCLUDED.role,
    deleted = EXCLUDED.deleted,
    last_updated = EXCLUDED.last_updated;

WITH clock AS (
    SELECT now_ts FROM _seed_clock
)
INSERT INTO appointments (
    id, start_time, end_time, type, status, created_at, creator_id, last_updated,
    canceller_id, cancellation_initiator, cancel_reason, cancelled_at,
    confirm_receptionist_id, confirmed_at, doctor_id, patient_id, version
)
SELECT
    'a1000000-0000-0000-0000-000000000001'::uuid,
    now_ts + interval '4 minutes',
    now_ts + interval '49 minutes',
    'QUICK_CHECK',
    'CONFIRMED',
    now_ts - interval '2 days',
    '13f247e0-ca31-4240-a9c8-3207e6ec87e7'::uuid,
    now_ts - interval '2 days',
    NULL::uuid, NULL::varchar(255), NULL::varchar(255), NULL::timestamp(6),
    NULL::uuid, NULL::timestamp(6),
    '863dc859-a714-4bee-91de-8cbe72c5aa08'::uuid,
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    0
FROM clock
UNION ALL
SELECT
    'a1000000-0000-0000-0000-000000000002'::uuid,
    now_ts - interval '30 minutes',
    now_ts + interval '15 minutes',
    'QUICK_CHECK',
    'CONFIRMED',
    now_ts - interval '1 day',
    '13f247e0-ca31-4240-a9c8-3207e6ec87e7'::uuid,
    now_ts - interval '1 day',
    NULL::uuid, NULL::varchar(255), NULL::varchar(255), NULL::timestamp(6),
    NULL::uuid, NULL::timestamp(6),
    '863dc859-a714-4bee-91de-8cbe72c5aa08'::uuid,
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    0
FROM clock
UNION ALL
SELECT
    'a1000000-0000-0000-0000-000000000003'::uuid,
    date_trunc('day', now_ts) - interval '1 day' + interval '10 hours',
    date_trunc('day', now_ts) - interval '1 day' + interval '10 hours' + interval '45 minutes',
    'QUICK_CHECK',
    'COMPLETED',
    date_trunc('day', now_ts) - interval '8 days',
    '13f247e0-ca31-4240-a9c8-3207e6ec87e7'::uuid,
    date_trunc('day', now_ts) - interval '1 day' + interval '10 hours' + interval '4 minutes',
    NULL::uuid, NULL::varchar(255), NULL::varchar(255), NULL::timestamp(6),
    '13f247e0-ca31-4240-a9c8-3207e6ec87e7'::uuid,
    date_trunc('day', now_ts) - interval '1 day' + interval '10 hours' + interval '4 minutes',
    '863dc859-a714-4bee-91de-8cbe72c5aa08'::uuid,
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    0
FROM clock
UNION ALL
SELECT
    'a1000000-0000-0000-0000-000000000004'::uuid,
    now_ts - interval '10 days',
    now_ts - interval '10 days' + interval '75 minutes',
    'MID_CHECK',
    'COMPLETED',
    now_ts - interval '20 days',
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    now_ts - interval '10 days' + interval '5 minutes',
    NULL::uuid, NULL::varchar(255), NULL::varchar(255), NULL::timestamp(6),
    '13f247e0-ca31-4240-a9c8-3207e6ec87e7'::uuid,
    now_ts - interval '10 days' + interval '5 minutes',
    '863dc859-a714-4bee-91de-8cbe72c5aa08'::uuid,
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    0
FROM clock
UNION ALL
SELECT
    'a1000000-0000-0000-0000-000000000005'::uuid,
    now_ts + interval '2 days',
    now_ts + interval '2 days' + interval '105 minutes',
    'LONG_CHECK',
    'CANCELLED',
    now_ts - interval '2 days',
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    now_ts - interval '6 hours',
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    'PATIENT',
    'Cancelled for test scenario',
    now_ts - interval '6 hours',
    NULL::uuid, NULL::timestamp(6),
    '863dc859-a714-4bee-91de-8cbe72c5aa08'::uuid,
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    0
FROM clock
UNION ALL
SELECT
    'a1000000-0000-0000-0000-000000000006'::uuid,
    now_ts + interval '5 days',
    now_ts + interval '5 days' + interval '45 minutes',
    'QUICK_CHECK',
    'CONFIRMED',
    now_ts - interval '12 hours',
    '13f247e0-ca31-4240-a9c8-3207e6ec87e7'::uuid,
    now_ts - interval '12 hours',
    NULL::uuid, NULL::varchar(255), NULL::varchar(255), NULL::timestamp(6),
    NULL::uuid, NULL::timestamp(6),
    '863dc859-a714-4bee-91de-8cbe72c5aa08'::uuid,
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    0
FROM clock
ON CONFLICT (id) DO UPDATE
SET
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    type = EXCLUDED.type,
    status = EXCLUDED.status,
    created_at = EXCLUDED.created_at,
    creator_id = EXCLUDED.creator_id,
    last_updated = EXCLUDED.last_updated,
    canceller_id = EXCLUDED.canceller_id,
    cancellation_initiator = EXCLUDED.cancellation_initiator,
    cancel_reason = EXCLUDED.cancel_reason,
    cancelled_at = EXCLUDED.cancelled_at,
    confirm_receptionist_id = EXCLUDED.confirm_receptionist_id,
    confirmed_at = EXCLUDED.confirmed_at,
    doctor_id = EXCLUDED.doctor_id,
    patient_id = EXCLUDED.patient_id,
    version = EXCLUDED.version;

WITH clock AS (
    SELECT now_ts FROM _seed_clock
)
INSERT INTO patient_record_access (
    id, record_type, patient_id, doctor_id, status, created_at, last_updated
)
SELECT
    'b1000000-0000-0000-0000-000000000001'::uuid,
    'PRECHECK',
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    '863dc859-a714-4bee-91de-8cbe72c5aa08'::uuid,
    'APPROVED',
    now_ts - interval '15 days',
    now_ts - interval '14 days'
FROM clock
UNION ALL
SELECT
    'b1000000-0000-0000-0000-000000000002'::uuid,
    'LAB_REQUEST',
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    '863dc859-a714-4bee-91de-8cbe72c5aa08'::uuid,
    'PENDING',
    now_ts - interval '1 day',
    now_ts - interval '1 day'
FROM clock
UNION ALL
SELECT
    'b1000000-0000-0000-0000-000000000003'::uuid,
    'PRESCRIPTION',
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    '863dc859-a714-4bee-91de-8cbe72c5aa08'::uuid,
    'PENDING',
    now_ts - interval '2 hours',
    now_ts - interval '2 hours'
FROM clock
ON CONFLICT (id) DO UPDATE
SET
    record_type = EXCLUDED.record_type,
    patient_id = EXCLUDED.patient_id,
    doctor_id = EXCLUDED.doctor_id,
    status = EXCLUDED.status,
    created_at = EXCLUDED.created_at,
    last_updated = EXCLUDED.last_updated;

WITH clock AS (
    SELECT now_ts FROM _seed_clock
)
INSERT INTO prechecks (
    id, appointment_id, patient_id, doctor_id, nurse_id, status,
    pulse, sugar, temperature, height, weight, note, created_at, last_updated, version
)
SELECT
    'c1000000-0000-0000-0000-000000000001'::uuid,
    'a1000000-0000-0000-0000-000000000004'::uuid,
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    '863dc859-a714-4bee-91de-8cbe72c5aa08'::uuid,
    'ac8337d4-e4f9-48cb-8822-17d827d07f71'::uuid,
    'VALID',
    74, 5.1, 36.7, 178.0, 82.4,
    'Baseline precheck for historical completed appointment',
    now_ts - interval '10 days' + interval '10 minutes',
    now_ts - interval '10 days' + interval '10 minutes',
    0
FROM clock
ON CONFLICT (id) DO UPDATE
SET
    appointment_id = EXCLUDED.appointment_id,
    patient_id = EXCLUDED.patient_id,
    doctor_id = EXCLUDED.doctor_id,
    nurse_id = EXCLUDED.nurse_id,
    status = EXCLUDED.status,
    pulse = EXCLUDED.pulse,
    sugar = EXCLUDED.sugar,
    temperature = EXCLUDED.temperature,
    height = EXCLUDED.height,
    weight = EXCLUDED.weight,
    note = EXCLUDED.note,
    created_at = EXCLUDED.created_at,
    last_updated = EXCLUDED.last_updated,
    version = EXCLUDED.version;

WITH clock AS (
    SELECT now_ts FROM _seed_clock
)
INSERT INTO lab_requests (
    id, status, created_at, last_updated, version, doctor_id, patient_id, appointment_id
)
SELECT
    'd1000000-0000-0000-0000-000000000001'::uuid,
    'REQUESTED',
    now_ts - interval '30 minutes',
    now_ts - interval '30 minutes',
    0,
    '863dc859-a714-4bee-91de-8cbe72c5aa08'::uuid,
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    'a1000000-0000-0000-0000-000000000003'::uuid
FROM clock
UNION ALL
SELECT
    'd1000000-0000-0000-0000-000000000002'::uuid,
    'IN_PROGRESS',
    now_ts - interval '8 days',
    now_ts - interval '1 day',
    0,
    '863dc859-a714-4bee-91de-8cbe72c5aa08'::uuid,
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    'a1000000-0000-0000-0000-000000000004'::uuid
FROM clock
UNION ALL
SELECT
    'd1000000-0000-0000-0000-000000000003'::uuid,
    'COMPLETED',
    now_ts - interval '7 days',
    now_ts - interval '6 days',
    0,
    '863dc859-a714-4bee-91de-8cbe72c5aa08'::uuid,
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    'a1000000-0000-0000-0000-000000000004'::uuid
FROM clock
ON CONFLICT (id) DO UPDATE
SET
    status = EXCLUDED.status,
    created_at = EXCLUDED.created_at,
    last_updated = EXCLUDED.last_updated,
    version = EXCLUDED.version,
    doctor_id = EXCLUDED.doctor_id,
    patient_id = EXCLUDED.patient_id,
    appointment_id = EXCLUDED.appointment_id;

WITH clock AS (
    SELECT now_ts FROM _seed_clock
)
INSERT INTO lab_tests (
    id, status, code, name, result, unit, lab_technician_note, doctor_note,
    created_at, last_updated, lab_technician_id, lab_request_id, patient_id, version
)
SELECT
    'e1000000-0000-0000-0000-000000000001'::uuid,
    'REQUESTED',
    'CRP-TEST-01',
    'C-Reactive Protein',
    NULL,
    'mg/L',
    NULL,
    'Inflammation follow-up',
    now_ts - interval '30 minutes',
    now_ts - interval '30 minutes',
    NULL,
    'd1000000-0000-0000-0000-000000000001'::uuid,
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    0
FROM clock
UNION ALL
SELECT
    'e1000000-0000-0000-0000-000000000002'::uuid,
    'IN_PROGRESS',
    'LIPID-FAST-01',
    'Fasting Lipid Panel',
    NULL,
    'mg/dL',
    'Sample received, processing in analyzer',
    'Cardio risk baseline',
    now_ts - interval '8 days' + interval '30 minutes',
    now_ts - interval '1 day',
    '1f834870-ee09-4c54-89c6-14669f83f2c7'::uuid,
    'd1000000-0000-0000-0000-000000000002'::uuid,
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    0
FROM clock
UNION ALL
SELECT
    'e1000000-0000-0000-0000-000000000003'::uuid,
    'COMPLETED',
    'CBC-STD-01',
    'Complete Blood Count',
    '5.8',
    'x10^9/L',
    'Within reference range',
    'Routine annual panel',
    now_ts - interval '7 days',
    now_ts - interval '6 days' + interval '2 hours',
    '1f834870-ee09-4c54-89c6-14669f83f2c7'::uuid,
    'd1000000-0000-0000-0000-000000000003'::uuid,
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    0
FROM clock
ON CONFLICT (id) DO UPDATE
SET
    status = EXCLUDED.status,
    code = EXCLUDED.code,
    name = EXCLUDED.name,
    result = EXCLUDED.result,
    unit = EXCLUDED.unit,
    lab_technician_note = EXCLUDED.lab_technician_note,
    doctor_note = EXCLUDED.doctor_note,
    created_at = EXCLUDED.created_at,
    last_updated = EXCLUDED.last_updated,
    lab_technician_id = EXCLUDED.lab_technician_id,
    lab_request_id = EXCLUDED.lab_request_id,
    patient_id = EXCLUDED.patient_id,
    version = EXCLUDED.version;

WITH clock AS (
    SELECT now_ts FROM _seed_clock
)
INSERT INTO prescriptions (
    id, status, doctor_id, patient_id, appointment_id, start_date, end_date,
    total_refills, remaining_refills, refill_interval_days,
    next_eligible_refill_at, last_consumed_at, general_note,
    created_at, last_updated, version
)
SELECT
    'f1000000-0000-0000-0000-000000000001'::uuid,
    'ACTIVE',
    '863dc859-a714-4bee-91de-8cbe72c5aa08'::uuid,
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    'a1000000-0000-0000-0000-000000000004'::uuid,
    now_ts::date - 2,
    now_ts::date + 60,
    3,
    2,
    7,
    now_ts - interval '10 minutes',
    now_ts - interval '7 days',
    'Eligible refill now for consume-refill test',
    now_ts - interval '1 day',
    now_ts - interval '1 day',
    0
FROM clock
UNION ALL
SELECT
    'f1000000-0000-0000-0000-000000000002'::uuid,
    'ACTIVE',
    '863dc859-a714-4bee-91de-8cbe72c5aa08'::uuid,
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    'a1000000-0000-0000-0000-000000000004'::uuid,
    now_ts::date,
    now_ts::date + 45,
    2,
    1,
    30,
    now_ts + interval '2 days',
    now_ts - interval '1 day',
    'Not yet eligible refill test',
    now_ts - interval '12 hours',
    now_ts - interval '12 hours',
    0
FROM clock
UNION ALL
SELECT
    'f1000000-0000-0000-0000-000000000003'::uuid,
    'EXPIRED',
    '863dc859-a714-4bee-91de-8cbe72c5aa08'::uuid,
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    'a1000000-0000-0000-0000-000000000004'::uuid,
    now_ts::date - 40,
    now_ts::date - 5,
    1,
    1,
    30,
    now_ts - interval '10 days',
    now_ts - interval '12 days',
    'Expired prescription test fixture',
    now_ts - interval '20 days',
    now_ts - interval '5 days',
    0
FROM clock
ON CONFLICT (id) DO UPDATE
SET
    status = EXCLUDED.status,
    doctor_id = EXCLUDED.doctor_id,
    patient_id = EXCLUDED.patient_id,
    appointment_id = EXCLUDED.appointment_id,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    total_refills = EXCLUDED.total_refills,
    remaining_refills = EXCLUDED.remaining_refills,
    refill_interval_days = EXCLUDED.refill_interval_days,
    next_eligible_refill_at = EXCLUDED.next_eligible_refill_at,
    last_consumed_at = EXCLUDED.last_consumed_at,
    general_note = EXCLUDED.general_note,
    created_at = EXCLUDED.created_at,
    last_updated = EXCLUDED.last_updated,
    version = EXCLUDED.version;

WITH clock AS (
    SELECT now_ts FROM _seed_clock
)
INSERT INTO prescription_medicines (
    id, prescription_id, medicine_name, dosage, frequency, route, instructions, quantity, created_at, last_updated
)
SELECT
    'aa000000-0000-0000-0000-000000000001'::uuid,
    'f1000000-0000-0000-0000-000000000001'::uuid,
    'Ibuprofen',
    '200mg',
    'Twice daily',
    'Oral',
    'Take with food',
    '30 tablets',
    now_ts - interval '1 day',
    now_ts - interval '1 day'
FROM clock
UNION ALL
SELECT
    'aa000000-0000-0000-0000-000000000002'::uuid,
    'f1000000-0000-0000-0000-000000000002'::uuid,
    'Atorvastatin',
    '10mg',
    'Once nightly',
    'Oral',
    'Take after dinner',
    '30 tablets',
    now_ts - interval '12 hours',
    now_ts - interval '12 hours'
FROM clock
UNION ALL
SELECT
    'aa000000-0000-0000-0000-000000000003'::uuid,
    'f1000000-0000-0000-0000-000000000003'::uuid,
    'Amoxicillin',
    '500mg',
    'Three times daily',
    'Oral',
    'Complete full course',
    '21 capsules',
    now_ts - interval '20 days',
    now_ts - interval '20 days'
FROM clock
ON CONFLICT (id) DO UPDATE
SET
    prescription_id = EXCLUDED.prescription_id,
    medicine_name = EXCLUDED.medicine_name,
    dosage = EXCLUDED.dosage,
    frequency = EXCLUDED.frequency,
    route = EXCLUDED.route,
    instructions = EXCLUDED.instructions,
    quantity = EXCLUDED.quantity,
    created_at = EXCLUDED.created_at,
    last_updated = EXCLUDED.last_updated;

WITH clock AS (
    SELECT now_ts FROM _seed_clock
)
INSERT INTO appointment_bills (
    id, amount, insurance_cover_amount, patient_payment_amount, status, type,
    patient_id, confirm_accountant_id, paid_on, created_at, last_updated, version, appointment_id
)
SELECT
    '91000000-0000-0000-0000-000000000001'::uuid,
    20.00,
    0.00,
    20.00,
    'UNPAID',
    'MID_CHECK_FEE',
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    NULL::uuid,
    now_ts - interval '49 days',
    now_ts - interval '9 days',
    now_ts - interval '9 days',
    0,
    'a1000000-0000-0000-0000-000000000004'::uuid
FROM clock
UNION ALL
SELECT
    '91000000-0000-0000-0000-000000000002'::uuid,
    15.00,
    NULL::numeric(38,2),
    NULL::numeric(38,2),
    'VIEWING',
    'QUICK_CHECK_FEE',
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    NULL::uuid,
    NULL::timestamp(6),
    now_ts - interval '10 minutes',
    now_ts - interval '10 minutes',
    0,
    'a1000000-0000-0000-0000-000000000001'::uuid
FROM clock
UNION ALL
SELECT
    '91000000-0000-0000-0000-000000000003'::uuid,
    15.00,
    5.00,
    10.00,
    'PAID',
    'QUICK_CHECK_FEE',
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    '2c354b57-9111-4806-a365-10ba3337d2fc'::uuid,
    now_ts - interval '2 hours',
    now_ts - interval '3 hours',
    now_ts - interval '2 hours',
    0,
    'a1000000-0000-0000-0000-000000000003'::uuid
FROM clock
UNION ALL
SELECT
    '91000000-0000-0000-0000-000000000004'::uuid,
    1.25,
    0.00,
    1.25,
    'UNPAID',
    'LONG_CHECK_CANCELLATION_FEE',
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    NULL::uuid,
    NULL::timestamp(6),
    now_ts - interval '6 hours',
    now_ts - interval '6 hours',
    0,
    'a1000000-0000-0000-0000-000000000005'::uuid
FROM clock
ON CONFLICT (id) DO UPDATE
SET
    amount = EXCLUDED.amount,
    insurance_cover_amount = EXCLUDED.insurance_cover_amount,
    patient_payment_amount = EXCLUDED.patient_payment_amount,
    status = EXCLUDED.status,
    type = EXCLUDED.type,
    patient_id = EXCLUDED.patient_id,
    confirm_accountant_id = EXCLUDED.confirm_accountant_id,
    paid_on = EXCLUDED.paid_on,
    created_at = EXCLUDED.created_at,
    last_updated = EXCLUDED.last_updated,
    version = EXCLUDED.version,
    appointment_id = EXCLUDED.appointment_id;

WITH clock AS (
    SELECT now_ts FROM _seed_clock
)
INSERT INTO lab_bills (
    id, amount, insurance_cover_amount, patient_payment_amount, status, type,
    patient_id, confirm_accountant_id, paid_on, created_at, last_updated, version, lab_test_id
)
SELECT
    '92000000-0000-0000-0000-000000000001'::uuid,
    15.00,
    0.00,
    15.00,
    'UNPAID',
    'LAB_TEST_FEE',
    '3376dfb3-77fb-493d-91c1-2e37ef3747f1'::uuid,
    NULL,
    NULL,
    now_ts - interval '5 days',
    now_ts - interval '5 days',
    0,
    'e1000000-0000-0000-0000-000000000003'::uuid
FROM clock
ON CONFLICT (id) DO UPDATE
SET
    amount = EXCLUDED.amount,
    insurance_cover_amount = EXCLUDED.insurance_cover_amount,
    patient_payment_amount = EXCLUDED.patient_payment_amount,
    status = EXCLUDED.status,
    type = EXCLUDED.type,
    patient_id = EXCLUDED.patient_id,
    confirm_accountant_id = EXCLUDED.confirm_accountant_id,
    paid_on = EXCLUDED.paid_on,
    created_at = EXCLUDED.created_at,
    last_updated = EXCLUDED.last_updated,
    version = EXCLUDED.version,
    lab_test_id = EXCLUDED.lab_test_id;

-- ------------------------------------------------------------
-- Persona-focused fixtures:
-- j.pinkman, recep.shilly, dr.grey, labtech, nurse.jared, acct.jarin
-- ------------------------------------------------------------

WITH clock AS (
    SELECT now_ts FROM _seed_clock
)
INSERT INTO appointments (
    id, start_time, end_time, type, status, created_at, creator_id, last_updated,
    canceller_id, cancellation_initiator, cancel_reason, cancelled_at,
    confirm_receptionist_id, confirmed_at, doctor_id, patient_id, version
)
SELECT
    'a2000000-0000-0000-0000-000000000001'::uuid,
    now_ts + interval '3 minutes',
    now_ts + interval '48 minutes',
    'QUICK_CHECK',
    'CONFIRMED',
    now_ts - interval '1 day',
    '1ce33e60-07ec-4ffa-8095-23d711ecf1a3'::uuid,
    now_ts - interval '1 day',
    NULL::uuid, NULL::varchar(255), NULL::varchar(255), NULL::timestamp(6),
    NULL::uuid, NULL::timestamp(6),
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    0
FROM clock
UNION ALL
SELECT
    'a2000000-0000-0000-0000-000000000002'::uuid,
    date_trunc('day', now_ts) - interval '2 days' + interval '11 hours',
    date_trunc('day', now_ts) - interval '2 days' + interval '11 hours' + interval '45 minutes',
    'QUICK_CHECK',
    'COMPLETED',
    date_trunc('day', now_ts) - interval '9 days',
    '1ce33e60-07ec-4ffa-8095-23d711ecf1a3'::uuid,
    date_trunc('day', now_ts) - interval '2 days' + interval '11 hours' + interval '6 minutes',
    NULL::uuid, NULL::varchar(255), NULL::varchar(255), NULL::timestamp(6),
    '1ce33e60-07ec-4ffa-8095-23d711ecf1a3'::uuid,
    date_trunc('day', now_ts) - interval '2 days' + interval '11 hours' + interval '6 minutes',
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    0
FROM clock
UNION ALL
SELECT
    'a2000000-0000-0000-0000-000000000003'::uuid,
    now_ts - interval '7 days',
    now_ts - interval '7 days' + interval '75 minutes',
    'MID_CHECK',
    'COMPLETED',
    now_ts - interval '18 days',
    '1ce33e60-07ec-4ffa-8095-23d711ecf1a3'::uuid,
    now_ts - interval '7 days' + interval '7 minutes',
    NULL::uuid, NULL::varchar(255), NULL::varchar(255), NULL::timestamp(6),
    '1ce33e60-07ec-4ffa-8095-23d711ecf1a3'::uuid,
    now_ts - interval '7 days' + interval '7 minutes',
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    0
FROM clock
UNION ALL
SELECT
    'a2000000-0000-0000-0000-000000000004'::uuid,
    now_ts + interval '3 days',
    now_ts + interval '3 days' + interval '75 minutes',
    'MID_CHECK',
    'CANCELLED',
    now_ts - interval '4 hours',
    '1ce33e60-07ec-4ffa-8095-23d711ecf1a3'::uuid,
    now_ts - interval '2 hours',
    '1ce33e60-07ec-4ffa-8095-23d711ecf1a3'::uuid,
    'RECEPTIONIST_ON_BEHALF_OF_PATIENT',
    'Rescheduled by receptionist for patient',
    now_ts - interval '2 hours',
    NULL::uuid,
    NULL::timestamp(6),
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    0
FROM clock
ON CONFLICT (id) DO UPDATE
SET
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    type = EXCLUDED.type,
    status = EXCLUDED.status,
    created_at = EXCLUDED.created_at,
    creator_id = EXCLUDED.creator_id,
    last_updated = EXCLUDED.last_updated,
    canceller_id = EXCLUDED.canceller_id,
    cancellation_initiator = EXCLUDED.cancellation_initiator,
    cancel_reason = EXCLUDED.cancel_reason,
    cancelled_at = EXCLUDED.cancelled_at,
    confirm_receptionist_id = EXCLUDED.confirm_receptionist_id,
    confirmed_at = EXCLUDED.confirmed_at,
    doctor_id = EXCLUDED.doctor_id,
    patient_id = EXCLUDED.patient_id,
    version = EXCLUDED.version;

WITH clock AS (
    SELECT now_ts FROM _seed_clock
)
INSERT INTO prechecks (
    id, appointment_id, patient_id, doctor_id, nurse_id, status,
    pulse, sugar, temperature, height, weight, note, created_at, last_updated, version
)
SELECT
    'c2000000-0000-0000-0000-000000000001'::uuid,
    'a2000000-0000-0000-0000-000000000002'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '6f84464f-73f4-48a1-bbbc-cb668d4ebc37'::uuid,
    'VALID',
    89, 5.4, 36.8, 172.0, 67.8,
    'Current-window precheck by nurse.jared for j.pinkman',
    now_ts - interval '5 minutes',
    now_ts - interval '5 minutes',
    0
FROM clock
UNION ALL
SELECT
    'c2000000-0000-0000-0000-000000000002'::uuid,
    'a2000000-0000-0000-0000-000000000003'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '6f84464f-73f4-48a1-bbbc-cb668d4ebc37'::uuid,
    'VALID',
    84, 5.2, 36.7, 172.0, 68.1,
    'Historical precheck by nurse.jared for j.pinkman',
    now_ts - interval '7 days' + interval '10 minutes',
    now_ts - interval '7 days' + interval '10 minutes',
    0
FROM clock
ON CONFLICT (id) DO UPDATE
SET
    appointment_id = EXCLUDED.appointment_id,
    patient_id = EXCLUDED.patient_id,
    doctor_id = EXCLUDED.doctor_id,
    nurse_id = EXCLUDED.nurse_id,
    status = EXCLUDED.status,
    pulse = EXCLUDED.pulse,
    sugar = EXCLUDED.sugar,
    temperature = EXCLUDED.temperature,
    height = EXCLUDED.height,
    weight = EXCLUDED.weight,
    note = EXCLUDED.note,
    created_at = EXCLUDED.created_at,
    last_updated = EXCLUDED.last_updated,
    version = EXCLUDED.version;

WITH clock AS (
    SELECT now_ts FROM _seed_clock
)
INSERT INTO lab_requests (
    id, status, created_at, last_updated, version, doctor_id, patient_id, appointment_id
)
SELECT
    'd2000000-0000-0000-0000-000000000001'::uuid,
    'IN_PROGRESS',
    now_ts - interval '15 minutes',
    now_ts - interval '10 minutes',
    0,
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    'a2000000-0000-0000-0000-000000000002'::uuid
FROM clock
UNION ALL
SELECT
    'd2000000-0000-0000-0000-000000000002'::uuid,
    'COMPLETED',
    now_ts - interval '7 days' + interval '35 minutes',
    now_ts - interval '7 days' + interval '3 hours',
    0,
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    'a2000000-0000-0000-0000-000000000003'::uuid
FROM clock
ON CONFLICT (id) DO UPDATE
SET
    status = EXCLUDED.status,
    created_at = EXCLUDED.created_at,
    last_updated = EXCLUDED.last_updated,
    version = EXCLUDED.version,
    doctor_id = EXCLUDED.doctor_id,
    patient_id = EXCLUDED.patient_id,
    appointment_id = EXCLUDED.appointment_id;

WITH clock AS (
    SELECT now_ts FROM _seed_clock
)
INSERT INTO lab_tests (
    id, status, code, name, result, unit, lab_technician_note, doctor_note,
    created_at, last_updated, lab_technician_id, lab_request_id, patient_id, version
)
SELECT
    'e2000000-0000-0000-0000-000000000001'::uuid,
    'IN_PROGRESS',
    'A1C-PINK-01',
    'HbA1c',
    NULL::varchar(255),
    '%',
    'Sample loaded, calibration running',
    'Evaluate glucose trend',
    now_ts - interval '15 minutes',
    now_ts - interval '10 minutes',
    '1f834870-ee09-4c54-89c6-14669f83f2c7'::uuid,
    'd2000000-0000-0000-0000-000000000001'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    0
FROM clock
UNION ALL
SELECT
    'e2000000-0000-0000-0000-000000000002'::uuid,
    'COMPLETED',
    'CMP-PINK-01',
    'Comprehensive Metabolic Panel',
    'Normal',
    'panel',
    'Completed by labtech with no critical values',
    'Routine post-visit lab',
    now_ts - interval '7 days' + interval '35 minutes',
    now_ts - interval '7 days' + interval '2 hours',
    '1f834870-ee09-4c54-89c6-14669f83f2c7'::uuid,
    'd2000000-0000-0000-0000-000000000002'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    0
FROM clock
ON CONFLICT (id) DO UPDATE
SET
    status = EXCLUDED.status,
    code = EXCLUDED.code,
    name = EXCLUDED.name,
    result = EXCLUDED.result,
    unit = EXCLUDED.unit,
    lab_technician_note = EXCLUDED.lab_technician_note,
    doctor_note = EXCLUDED.doctor_note,
    created_at = EXCLUDED.created_at,
    last_updated = EXCLUDED.last_updated,
    lab_technician_id = EXCLUDED.lab_technician_id,
    lab_request_id = EXCLUDED.lab_request_id,
    patient_id = EXCLUDED.patient_id,
    version = EXCLUDED.version;

WITH clock AS (
    SELECT now_ts FROM _seed_clock
)
INSERT INTO prescriptions (
    id, status, doctor_id, patient_id, appointment_id, start_date, end_date,
    total_refills, remaining_refills, refill_interval_days,
    next_eligible_refill_at, last_consumed_at, general_note, created_at, last_updated, version
)
SELECT
    'f2000000-0000-0000-0000-000000000001'::uuid,
    'ACTIVE',
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    'a2000000-0000-0000-0000-000000000003'::uuid,
    now_ts::date - 1,
    now_ts::date + 45,
    3,
    2,
    14,
    now_ts - interval '5 minutes',
    now_ts - interval '15 days',
    'dr.grey follow-up prescription for j.pinkman',
    now_ts - interval '7 days' + interval '80 minutes',
    now_ts - interval '7 days' + interval '80 minutes',
    0
FROM clock
UNION ALL
SELECT
    'f2000000-0000-0000-0000-000000000002'::uuid,
    'COMPLETED',
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    'a2000000-0000-0000-0000-000000000003'::uuid,
    now_ts::date - 90,
    now_ts::date - 30,
    1,
    0,
    30,
    now_ts - interval '49 days',
    now_ts - interval '50 days',
    'Historical completed prescription fixture',
    now_ts - interval '85 days',
    now_ts - interval '50 days',
    0
FROM clock
ON CONFLICT (id) DO UPDATE
SET
    status = EXCLUDED.status,
    doctor_id = EXCLUDED.doctor_id,
    patient_id = EXCLUDED.patient_id,
    appointment_id = EXCLUDED.appointment_id,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    total_refills = EXCLUDED.total_refills,
    remaining_refills = EXCLUDED.remaining_refills,
    refill_interval_days = EXCLUDED.refill_interval_days,
    next_eligible_refill_at = EXCLUDED.next_eligible_refill_at,
    last_consumed_at = EXCLUDED.last_consumed_at,
    general_note = EXCLUDED.general_note,
    created_at = EXCLUDED.created_at,
    last_updated = EXCLUDED.last_updated,
    version = EXCLUDED.version;

WITH clock AS (
    SELECT now_ts FROM _seed_clock
)
INSERT INTO prescription_medicines (
    id, prescription_id, medicine_name, dosage, frequency, route, instructions, quantity, created_at, last_updated
)
SELECT
    'aa200000-0000-0000-0000-000000000001'::uuid,
    'f2000000-0000-0000-0000-000000000001'::uuid,
    'Metformin',
    '500mg',
    'Twice daily',
    'Oral',
    'Take with meals',
    '60 tablets',
    now_ts - interval '7 days' + interval '80 minutes',
    now_ts - interval '7 days' + interval '80 minutes'
FROM clock
UNION ALL
SELECT
    'aa200000-0000-0000-0000-000000000002'::uuid,
    'f2000000-0000-0000-0000-000000000002'::uuid,
    'Amoxicillin',
    '500mg',
    'Three times daily',
    'Oral',
    'Finish the whole course',
    '21 capsules',
    now_ts - interval '85 days',
    now_ts - interval '85 days'
FROM clock
ON CONFLICT (id) DO UPDATE
SET
    prescription_id = EXCLUDED.prescription_id,
    medicine_name = EXCLUDED.medicine_name,
    dosage = EXCLUDED.dosage,
    frequency = EXCLUDED.frequency,
    route = EXCLUDED.route,
    instructions = EXCLUDED.instructions,
    quantity = EXCLUDED.quantity,
    created_at = EXCLUDED.created_at,
    last_updated = EXCLUDED.last_updated;

WITH clock AS (
    SELECT now_ts FROM _seed_clock
)
INSERT INTO appointment_bills (
    id, amount, insurance_cover_amount, patient_payment_amount, status, type,
    patient_id, confirm_accountant_id, paid_on, created_at, last_updated, version, appointment_id
)
SELECT
    '91000000-0000-0000-0000-000000000101'::uuid,
    20.00,
    8.00,
    12.00,
    'PAID',
    'MID_CHECK_FEE',
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    '2c354b57-9111-4806-a365-10ba3337d2fc'::uuid,
    now_ts - interval '6 days',
    now_ts - interval '7 days' + interval '7 minutes',
    now_ts - interval '6 days',
    0,
    'a2000000-0000-0000-0000-000000000003'::uuid
FROM clock
UNION ALL
SELECT
    '91000000-0000-0000-0000-000000000102'::uuid,
    1.00,
    0.00,
    1.00,
    'UNPAID',
    'MID_CHECK_CANCELLATION_FEE',
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    NULL::uuid,
    NULL::timestamp(6),
    now_ts - interval '2 hours',
    now_ts - interval '2 hours',
    0,
    'a2000000-0000-0000-0000-000000000004'::uuid
FROM clock
ON CONFLICT (id) DO UPDATE
SET
    amount = EXCLUDED.amount,
    insurance_cover_amount = EXCLUDED.insurance_cover_amount,
    patient_payment_amount = EXCLUDED.patient_payment_amount,
    status = EXCLUDED.status,
    type = EXCLUDED.type,
    patient_id = EXCLUDED.patient_id,
    confirm_accountant_id = EXCLUDED.confirm_accountant_id,
    paid_on = EXCLUDED.paid_on,
    created_at = EXCLUDED.created_at,
    last_updated = EXCLUDED.last_updated,
    version = EXCLUDED.version,
    appointment_id = EXCLUDED.appointment_id;

WITH clock AS (
    SELECT now_ts FROM _seed_clock
)
INSERT INTO lab_bills (
    id, amount, insurance_cover_amount, patient_payment_amount, status, type,
    patient_id, confirm_accountant_id, paid_on, created_at, last_updated, version, lab_test_id
)
SELECT
    '92000000-0000-0000-0000-000000000101'::uuid,
    15.00,
    5.00,
    10.00,
    'PAID',
    'LAB_TEST_FEE',
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    '2c354b57-9111-4806-a365-10ba3337d2fc'::uuid,
    now_ts - interval '6 days',
    now_ts - interval '7 days' + interval '2 hours',
    now_ts - interval '6 days',
    0,
    'e2000000-0000-0000-0000-000000000002'::uuid
FROM clock
ON CONFLICT (id) DO UPDATE
SET
    amount = EXCLUDED.amount,
    insurance_cover_amount = EXCLUDED.insurance_cover_amount,
    patient_payment_amount = EXCLUDED.patient_payment_amount,
    status = EXCLUDED.status,
    type = EXCLUDED.type,
    patient_id = EXCLUDED.patient_id,
    confirm_accountant_id = EXCLUDED.confirm_accountant_id,
    paid_on = EXCLUDED.paid_on,
    created_at = EXCLUDED.created_at,
    last_updated = EXCLUDED.last_updated,
    version = EXCLUDED.version,
    lab_test_id = EXCLUDED.lab_test_id;

-- ------------------------------------------------------------
-- Legacy detailed fixtures (adapted from postgres.sql.gz)
-- Timeline corrected: completed/no-show are in the past, upcoming are CONFIRMED.
-- ------------------------------------------------------------

INSERT INTO appointments (
    id, start_time, end_time, type, status, created_at, creator_id, last_updated,
    canceller_id, cancellation_initiator, cancel_reason, cancelled_at,
    confirm_receptionist_id, confirmed_at, doctor_id, patient_id, version
)
VALUES
(
    'a3000000-0000-0000-0000-000000000001'::uuid,
    (SELECT now_ts FROM _seed_clock) - interval '20 days' + interval '9 hours',
    (SELECT now_ts FROM _seed_clock) - interval '20 days' + interval '9 hours' + interval '45 minutes',
    'QUICK_CHECK',
    'COMPLETED',
    (SELECT now_ts FROM _seed_clock) - interval '30 days',
    '1ce33e60-07ec-4ffa-8095-23d711ecf1a3'::uuid,
    (SELECT now_ts FROM _seed_clock) - interval '20 days' + interval '9 hours' + interval '5 minutes',
    NULL,
    NULL,
    NULL,
    NULL,
    '1ce33e60-07ec-4ffa-8095-23d711ecf1a3'::uuid,
    (SELECT now_ts FROM _seed_clock) - interval '20 days' + interval '9 hours' + interval '5 minutes',
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    0
),
(
    'a3000000-0000-0000-0000-000000000002'::uuid,
    (SELECT now_ts FROM _seed_clock) - interval '18 days' + interval '10 hours',
    (SELECT now_ts FROM _seed_clock) - interval '18 days' + interval '10 hours' + interval '75 minutes',
    'MID_CHECK',
    'COMPLETED',
    (SELECT now_ts FROM _seed_clock) - interval '28 days',
    '1ce33e60-07ec-4ffa-8095-23d711ecf1a3'::uuid,
    (SELECT now_ts FROM _seed_clock) - interval '18 days' + interval '10 hours' + interval '8 minutes',
    NULL,
    NULL,
    NULL,
    NULL,
    '1ce33e60-07ec-4ffa-8095-23d711ecf1a3'::uuid,
    (SELECT now_ts FROM _seed_clock) - interval '18 days' + interval '10 hours' + interval '8 minutes',
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    0
),
(
    'a3000000-0000-0000-0000-000000000003'::uuid,
    (SELECT now_ts FROM _seed_clock) - interval '16 days' + interval '11 hours',
    (SELECT now_ts FROM _seed_clock) - interval '16 days' + interval '11 hours' + interval '105 minutes',
    'LONG_CHECK',
    'COMPLETED',
    (SELECT now_ts FROM _seed_clock) - interval '27 days',
    '1ce33e60-07ec-4ffa-8095-23d711ecf1a3'::uuid,
    (SELECT now_ts FROM _seed_clock) - interval '16 days' + interval '11 hours' + interval '6 minutes',
    NULL,
    NULL,
    NULL,
    NULL,
    '1ce33e60-07ec-4ffa-8095-23d711ecf1a3'::uuid,
    (SELECT now_ts FROM _seed_clock) - interval '16 days' + interval '11 hours' + interval '6 minutes',
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    0
),
(
    'a3000000-0000-0000-0000-000000000004'::uuid,
    (SELECT now_ts FROM _seed_clock) + interval '4 days' + interval '9 hours',
    (SELECT now_ts FROM _seed_clock) + interval '4 days' + interval '9 hours' + interval '45 minutes',
    'QUICK_CHECK',
    'CANCELLED',
    (SELECT now_ts FROM _seed_clock) - interval '3 days',
    '1ce33e60-07ec-4ffa-8095-23d711ecf1a3'::uuid,
    (SELECT now_ts FROM _seed_clock) - interval '1 day',
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    'PATIENT',
    'My goat ate my reminder note',
    (SELECT now_ts FROM _seed_clock) - interval '1 day',
    NULL,
    NULL,
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    0
),
(
    'a3000000-0000-0000-0000-000000000005'::uuid,
    (SELECT now_ts FROM _seed_clock) - interval '8 days' + interval '9 hours',
    (SELECT now_ts FROM _seed_clock) - interval '8 days' + interval '9 hours' + interval '45 minutes',
    'QUICK_CHECK',
    'NO_SHOW',
    (SELECT now_ts FROM _seed_clock) - interval '18 days',
    '1ce33e60-07ec-4ffa-8095-23d711ecf1a3'::uuid,
    (SELECT now_ts FROM _seed_clock) - interval '8 days' + interval '9 hours' + interval '20 minutes',
    '1ce33e60-07ec-4ffa-8095-23d711ecf1a3'::uuid,
    'RECEPTIONIST',
    'Patient did not show up',
    (SELECT now_ts FROM _seed_clock) - interval '8 days' + interval '9 hours' + interval '20 minutes',
    NULL,
    NULL,
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    0
),
(
    'a3000000-0000-0000-0000-000000000006'::uuid,
    (SELECT now_ts FROM _seed_clock) - interval '6 days' + interval '14 hours',
    (SELECT now_ts FROM _seed_clock) - interval '6 days' + interval '14 hours' + interval '45 minutes',
    'QUICK_CHECK',
    'COMPLETED',
    (SELECT now_ts FROM _seed_clock) - interval '15 days',
    '1ce33e60-07ec-4ffa-8095-23d711ecf1a3'::uuid,
    (SELECT now_ts FROM _seed_clock) - interval '6 days' + interval '14 hours' + interval '3 minutes',
    NULL,
    NULL,
    NULL,
    NULL,
    '1ce33e60-07ec-4ffa-8095-23d711ecf1a3'::uuid,
    (SELECT now_ts FROM _seed_clock) - interval '6 days' + interval '14 hours' + interval '3 minutes',
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    0
),
(
    'a3000000-0000-0000-0000-000000000007'::uuid,
    (SELECT now_ts FROM _seed_clock) - interval '4 days' + interval '13 hours',
    (SELECT now_ts FROM _seed_clock) - interval '4 days' + interval '13 hours' + interval '75 minutes',
    'MID_CHECK',
    'COMPLETED',
    (SELECT now_ts FROM _seed_clock) - interval '12 days',
    '1ce33e60-07ec-4ffa-8095-23d711ecf1a3'::uuid,
    (SELECT now_ts FROM _seed_clock) - interval '4 days' + interval '13 hours' + interval '4 minutes',
    NULL,
    NULL,
    NULL,
    NULL,
    '1ce33e60-07ec-4ffa-8095-23d711ecf1a3'::uuid,
    (SELECT now_ts FROM _seed_clock) - interval '4 days' + interval '13 hours' + interval '4 minutes',
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    0
),
(
    'a3000000-0000-0000-0000-000000000008'::uuid,
    (SELECT now_ts FROM _seed_clock) + interval '2 days' + interval '10 hours',
    (SELECT now_ts FROM _seed_clock) + interval '2 days' + interval '10 hours' + interval '105 minutes',
    'LONG_CHECK',
    'CONFIRMED',
    (SELECT now_ts FROM _seed_clock) - interval '2 days',
    '1ce33e60-07ec-4ffa-8095-23d711ecf1a3'::uuid,
    (SELECT now_ts FROM _seed_clock) - interval '2 days',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    0
)
ON CONFLICT (id) DO UPDATE
SET
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    type = EXCLUDED.type,
    status = EXCLUDED.status,
    created_at = EXCLUDED.created_at,
    creator_id = EXCLUDED.creator_id,
    last_updated = EXCLUDED.last_updated,
    canceller_id = EXCLUDED.canceller_id,
    cancellation_initiator = EXCLUDED.cancellation_initiator,
    cancel_reason = EXCLUDED.cancel_reason,
    cancelled_at = EXCLUDED.cancelled_at,
    confirm_receptionist_id = EXCLUDED.confirm_receptionist_id,
    confirmed_at = EXCLUDED.confirmed_at,
    doctor_id = EXCLUDED.doctor_id,
    patient_id = EXCLUDED.patient_id,
    version = EXCLUDED.version;

INSERT INTO prechecks (
    id, appointment_id, patient_id, doctor_id, nurse_id, status,
    pulse, sugar, temperature, height, weight, note, created_at, last_updated, version
)
VALUES
(
    'c3000000-0000-0000-0000-000000000001'::uuid,
    'a3000000-0000-0000-0000-000000000001'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '6f84464f-73f4-48a1-bbbc-cb668d4ebc37'::uuid,
    'VALID',
    78, 5.2, 36.7, 178.0, 82.5,
    'Patient cooperative, no complaints',
    (SELECT now_ts FROM _seed_clock) - interval '20 days' + interval '9 hours' + interval '10 minutes',
    (SELECT now_ts FROM _seed_clock) - interval '20 days' + interval '9 hours' + interval '10 minutes',
    0
),
(
    'c3000000-0000-0000-0000-000000000002'::uuid,
    'a3000000-0000-0000-0000-000000000002'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '6f84464f-73f4-48a1-bbbc-cb668d4ebc37'::uuid,
    'VALID',
    95, 4.8, 36.9, 172.0, 68.0,
    'Patient visibly anxious. Strong chemical smell noted.',
    (SELECT now_ts FROM _seed_clock) - interval '18 days' + interval '10 hours' + interval '12 minutes',
    (SELECT now_ts FROM _seed_clock) - interval '18 days' + interval '10 hours' + interval '12 minutes',
    0
),
(
    'c3000000-0000-0000-0000-000000000003'::uuid,
    'a3000000-0000-0000-0000-000000000003'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '6f84464f-73f4-48a1-bbbc-cb668d4ebc37'::uuid,
    'VALID',
    60, 5.1, 36.6, 190.5, 95.0,
    'Vitals suspiciously perfect. BP cuff broke on first attempt.',
    (SELECT now_ts FROM _seed_clock) - interval '16 days' + interval '11 hours' + interval '10 minutes',
    (SELECT now_ts FROM _seed_clock) - interval '16 days' + interval '11 hours' + interval '10 minutes',
    0
),
(
    'c3000000-0000-0000-0000-000000000004'::uuid,
    'a3000000-0000-0000-0000-000000000006'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '6f84464f-73f4-48a1-bbbc-cb668d4ebc37'::uuid,
    'VALID',
    85, 5.5, 37.0, 175.0, 88.0,
    'Patient smells strongly of farm animals. Otherwise unremarkable.',
    (SELECT now_ts FROM _seed_clock) - interval '6 days' + interval '14 hours' + interval '6 minutes',
    (SELECT now_ts FROM _seed_clock) - interval '6 days' + interval '14 hours' + interval '6 minutes',
    0
),
(
    'c3000000-0000-0000-0000-000000000005'::uuid,
    'a3000000-0000-0000-0000-000000000007'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '6f84464f-73f4-48a1-bbbc-cb668d4ebc37'::uuid,
    'VALID',
    98, 4.7, 37.1, 172.0, 67.5,
    'Weight down 500g since April. Jittery. Possible stimulant use.',
    (SELECT now_ts FROM _seed_clock) - interval '4 days' + interval '13 hours' + interval '8 minutes',
    (SELECT now_ts FROM _seed_clock) - interval '4 days' + interval '13 hours' + interval '8 minutes',
    0
)
ON CONFLICT (id) DO UPDATE
SET
    appointment_id = EXCLUDED.appointment_id,
    patient_id = EXCLUDED.patient_id,
    doctor_id = EXCLUDED.doctor_id,
    nurse_id = EXCLUDED.nurse_id,
    status = EXCLUDED.status,
    pulse = EXCLUDED.pulse,
    sugar = EXCLUDED.sugar,
    temperature = EXCLUDED.temperature,
    height = EXCLUDED.height,
    weight = EXCLUDED.weight,
    note = EXCLUDED.note,
    created_at = EXCLUDED.created_at,
    last_updated = EXCLUDED.last_updated,
    version = EXCLUDED.version;

INSERT INTO lab_requests (
    id, status, created_at, last_updated, version, doctor_id, patient_id, appointment_id
)
VALUES
(
    'd3000000-0000-0000-0000-000000000001'::uuid,
    'COMPLETED',
    (SELECT now_ts FROM _seed_clock) - interval '18 days' + interval '11 hours',
    (SELECT now_ts FROM _seed_clock) - interval '17 days' + interval '14 hours',
    0,
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    'a3000000-0000-0000-0000-000000000002'::uuid
),
(
    'd3000000-0000-0000-0000-000000000002'::uuid,
    'COMPLETED',
    (SELECT now_ts FROM _seed_clock) - interval '6 days' + interval '14 hours' + interval '30 minutes',
    (SELECT now_ts FROM _seed_clock) - interval '5 days' + interval '9 hours',
    0,
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    'a3000000-0000-0000-0000-000000000006'::uuid
),
(
    'd3000000-0000-0000-0000-000000000003'::uuid,
    'IN_PROGRESS',
    (SELECT now_ts FROM _seed_clock) - interval '4 days' + interval '14 hours',
    (SELECT now_ts FROM _seed_clock) - interval '3 days',
    0,
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    'a3000000-0000-0000-0000-000000000007'::uuid
)
ON CONFLICT (id) DO UPDATE
SET
    status = EXCLUDED.status,
    created_at = EXCLUDED.created_at,
    last_updated = EXCLUDED.last_updated,
    version = EXCLUDED.version,
    doctor_id = EXCLUDED.doctor_id,
    patient_id = EXCLUDED.patient_id,
    appointment_id = EXCLUDED.appointment_id;

INSERT INTO lab_tests (
    id, status, code, name, result, unit, lab_technician_note, doctor_note,
    created_at, last_updated, lab_technician_id, lab_request_id, patient_id, version
)
VALUES
(
    'e3000000-0000-0000-0000-000000000001'::uuid,
    'COMPLETED',
    'CBC-001',
    'Complete Blood Count',
    '11.8',
    'x10^9/L',
    'Minor WBC elevation noted. Otherwise unremarkable.',
    'Check for unusual compounds in blood',
    (SELECT now_ts FROM _seed_clock) - interval '18 days' + interval '11 hours',
    (SELECT now_ts FROM _seed_clock) - interval '17 days' + interval '10 hours',
    '1f834870-ee09-4c54-89c6-14669f83f2c7'::uuid,
    'd3000000-0000-0000-0000-000000000001'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    0
),
(
    'e3000000-0000-0000-0000-000000000002'::uuid,
    'COMPLETED',
    'HM-001',
    'Heavy Metal Screen',
    '7.9',
    'mcg/L',
    'Barium trace detected. Within acceptable limits. Barely.',
    'Rule out occupational exposure',
    (SELECT now_ts FROM _seed_clock) - interval '18 days' + interval '11 hours',
    (SELECT now_ts FROM _seed_clock) - interval '17 days' + interval '11 hours',
    '1f834870-ee09-4c54-89c6-14669f83f2c7'::uuid,
    'd3000000-0000-0000-0000-000000000001'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    0
),
(
    'e3000000-0000-0000-0000-000000000003'::uuid,
    'COMPLETED',
    'LIP-001',
    'Lipid Panel',
    '145',
    'mg/dL',
    'LDL slightly elevated. Recommend dietary review.',
    'Routine cardiovascular screen',
    (SELECT now_ts FROM _seed_clock) - interval '6 days' + interval '14 hours' + interval '30 minutes',
    (SELECT now_ts FROM _seed_clock) - interval '5 days' + interval '9 hours',
    '1f834870-ee09-4c54-89c6-14669f83f2c7'::uuid,
    'd3000000-0000-0000-0000-000000000002'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    0
),
(
    'e3000000-0000-0000-0000-000000000004'::uuid,
    'REQUESTED',
    'CMP-003',
    'Comprehensive Metabolic Panel',
    NULL,
    NULL,
    NULL,
    'Baseline metabolic workup. Patient evasive about diet and occupation.',
    (SELECT now_ts FROM _seed_clock) - interval '4 days' + interval '14 hours',
    (SELECT now_ts FROM _seed_clock) - interval '4 days' + interval '14 hours',
    NULL,
    'd3000000-0000-0000-0000-000000000003'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    0
),
(
    'e3000000-0000-0000-0000-000000000005'::uuid,
    'IN_PROGRESS',
    'TOX-002',
    'Extended Toxicology Screen',
    NULL,
    NULL,
    'Sample received. Running analysis. Unusual readings so far.',
    'Non-standard panel. Ordered at physician discretion. Do not ask why.',
    (SELECT now_ts FROM _seed_clock) - interval '4 days' + interval '14 hours',
    (SELECT now_ts FROM _seed_clock) - interval '3 days',
    '1f834870-ee09-4c54-89c6-14669f83f2c7'::uuid,
    'd3000000-0000-0000-0000-000000000003'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    0
)
ON CONFLICT (id) DO UPDATE
SET
    status = EXCLUDED.status,
    code = EXCLUDED.code,
    name = EXCLUDED.name,
    result = EXCLUDED.result,
    unit = EXCLUDED.unit,
    lab_technician_note = EXCLUDED.lab_technician_note,
    doctor_note = EXCLUDED.doctor_note,
    created_at = EXCLUDED.created_at,
    last_updated = EXCLUDED.last_updated,
    lab_technician_id = EXCLUDED.lab_technician_id,
    lab_request_id = EXCLUDED.lab_request_id,
    patient_id = EXCLUDED.patient_id,
    version = EXCLUDED.version;

INSERT INTO prescriptions (
    id, status, doctor_id, patient_id, appointment_id, start_date, end_date,
    total_refills, remaining_refills, refill_interval_days,
    next_eligible_refill_at, last_consumed_at, general_note, created_at, last_updated, version
)
VALUES
(
    'f3000000-0000-0000-0000-000000000001'::uuid,
    'ACTIVE',
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    'a3000000-0000-0000-0000-000000000002'::uuid,
    (SELECT now_ts::date FROM _seed_clock) - 20,
    (SELECT now_ts::date FROM _seed_clock) + 70,
    3,
    3,
    30,
    (SELECT now_ts FROM _seed_clock) - interval '1 day',
    NULL,
    'Take with food. Avoid operating heavy equipment. Or anything else.',
    (SELECT now_ts FROM _seed_clock) - interval '18 days' + interval '11 hours',
    (SELECT now_ts FROM _seed_clock) - interval '18 days' + interval '11 hours',
    0
),
(
    'f3000000-0000-0000-0000-000000000002'::uuid,
    'ACTIVE',
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    'a3000000-0000-0000-0000-000000000003'::uuid,
    (SELECT now_ts::date FROM _seed_clock) - 18,
    (SELECT now_ts::date FROM _seed_clock) + 95,
    5,
    4,
    30,
    (SELECT now_ts FROM _seed_clock) + interval '11 days',
    NULL,
    'Patient reviewed my medical license mid-appointment. I persevered.',
    (SELECT now_ts FROM _seed_clock) - interval '16 days' + interval '14 hours',
    (SELECT now_ts FROM _seed_clock) - interval '16 days' + interval '14 hours',
    0
),
(
    'f3000000-0000-0000-0000-000000000003'::uuid,
    'ACTIVE',
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    'a3000000-0000-0000-0000-000000000003'::uuid,
    (SELECT now_ts::date FROM _seed_clock) - 16,
    (SELECT now_ts::date FROM _seed_clock) + 120,
    2,
    2,
    30,
    (SELECT now_ts FROM _seed_clock) - interval '12 hours',
    NULL,
    'Bloodwork was perfect. Suspiciously so. Prescribed vitamins pending further investigation.',
    (SELECT now_ts FROM _seed_clock) - interval '16 days' + interval '15 hours',
    (SELECT now_ts FROM _seed_clock) - interval '16 days' + interval '15 hours',
    0
),
(
    'f3000000-0000-0000-0000-000000000004'::uuid,
    'COMPLETED',
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    'a3000000-0000-0000-0000-000000000001'::uuid,
    (SELECT now_ts::date FROM _seed_clock) - 120,
    (SELECT now_ts::date FROM _seed_clock) - 30,
    2,
    0,
    30,
    (SELECT now_ts FROM _seed_clock) - interval '40 days',
    (SELECT now_ts FROM _seed_clock) - interval '70 days',
    'Sleep disrupted by roommate. Recommended earplugs in addition to medication.',
    (SELECT now_ts FROM _seed_clock) - interval '110 days',
    (SELECT now_ts FROM _seed_clock) - interval '70 days',
    0
),
(
    'f3000000-0000-0000-0000-000000000005'::uuid,
    'ACTIVE',
    '9c1d7268-d215-4642-bff3-01f38f65ab3e'::uuid,
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    'a3000000-0000-0000-0000-000000000007'::uuid,
    (SELECT now_ts::date FROM _seed_clock) - 4,
    (SELECT now_ts::date FROM _seed_clock) + 70,
    1,
    1,
    30,
    (SELECT now_ts FROM _seed_clock) - interval '2 hours',
    NULL,
    'Patient denied everything. Prescribed anyway. Ordered toxicology.',
    (SELECT now_ts FROM _seed_clock) - interval '4 days' + interval '14 hours',
    (SELECT now_ts FROM _seed_clock) - interval '4 days' + interval '14 hours',
    0
)
ON CONFLICT (id) DO UPDATE
SET
    status = EXCLUDED.status,
    doctor_id = EXCLUDED.doctor_id,
    patient_id = EXCLUDED.patient_id,
    appointment_id = EXCLUDED.appointment_id,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    total_refills = EXCLUDED.total_refills,
    remaining_refills = EXCLUDED.remaining_refills,
    refill_interval_days = EXCLUDED.refill_interval_days,
    next_eligible_refill_at = EXCLUDED.next_eligible_refill_at,
    last_consumed_at = EXCLUDED.last_consumed_at,
    general_note = EXCLUDED.general_note,
    created_at = EXCLUDED.created_at,
    last_updated = EXCLUDED.last_updated,
    version = EXCLUDED.version;

INSERT INTO prescription_medicines (
    id, prescription_id, medicine_name, dosage, frequency, route, instructions, quantity, created_at, last_updated
)
VALUES
(
    'aa300000-0000-0000-0000-000000000001'::uuid,
    'f3000000-0000-0000-0000-000000000001'::uuid,
    'Ibuprofen',
    '400mg',
    'Twice daily',
    'Oral',
    'Take with food or milk',
    '60 tablets',
    (SELECT now_ts FROM _seed_clock) - interval '18 days' + interval '11 hours',
    (SELECT now_ts FROM _seed_clock) - interval '18 days' + interval '11 hours'
),
(
    'aa300000-0000-0000-0000-000000000002'::uuid,
    'f3000000-0000-0000-0000-000000000002'::uuid,
    'Cetirizine',
    '10mg',
    'Once daily',
    'Oral',
    'Take in the evening',
    '30 tablets',
    (SELECT now_ts FROM _seed_clock) - interval '16 days' + interval '14 hours',
    (SELECT now_ts FROM _seed_clock) - interval '16 days' + interval '14 hours'
),
(
    'aa300000-0000-0000-0000-000000000003'::uuid,
    'f3000000-0000-0000-0000-000000000002'::uuid,
    'Vitamin D3',
    '2000 IU',
    'Once daily',
    'Oral',
    'Take with a meal',
    '180 capsules',
    (SELECT now_ts FROM _seed_clock) - interval '16 days' + interval '14 hours',
    (SELECT now_ts FROM _seed_clock) - interval '16 days' + interval '14 hours'
),
(
    'aa300000-0000-0000-0000-000000000004'::uuid,
    'f3000000-0000-0000-0000-000000000003'::uuid,
    'Multivitamin',
    '1 tablet',
    'Once daily',
    'Oral',
    'Take with breakfast',
    '180 tablets',
    (SELECT now_ts FROM _seed_clock) - interval '16 days' + interval '15 hours',
    (SELECT now_ts FROM _seed_clock) - interval '16 days' + interval '15 hours'
),
(
    'aa300000-0000-0000-0000-000000000005'::uuid,
    'f3000000-0000-0000-0000-000000000004'::uuid,
    'Melatonin',
    '5mg',
    'Once nightly',
    'Oral',
    'Take 30 minutes before bed',
    '90 tablets',
    (SELECT now_ts FROM _seed_clock) - interval '110 days',
    (SELECT now_ts FROM _seed_clock) - interval '110 days'
),
(
    'aa300000-0000-0000-0000-000000000006'::uuid,
    'f3000000-0000-0000-0000-000000000005'::uuid,
    'Naltrexone',
    '50mg',
    'Once daily',
    'Oral',
    'Take at the same time each day',
    '30 tablets',
    (SELECT now_ts FROM _seed_clock) - interval '4 days' + interval '14 hours',
    (SELECT now_ts FROM _seed_clock) - interval '4 days' + interval '14 hours'
)
ON CONFLICT (id) DO UPDATE
SET
    prescription_id = EXCLUDED.prescription_id,
    medicine_name = EXCLUDED.medicine_name,
    dosage = EXCLUDED.dosage,
    frequency = EXCLUDED.frequency,
    route = EXCLUDED.route,
    instructions = EXCLUDED.instructions,
    quantity = EXCLUDED.quantity,
    created_at = EXCLUDED.created_at,
    last_updated = EXCLUDED.last_updated;

INSERT INTO patient_record_access (
    id, record_type, patient_id, doctor_id, status, created_at, last_updated
)
VALUES
(
    'b3000000-0000-0000-0000-000000000001'::uuid,
    'PRESCRIPTION',
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    'fdc76d3e-0e13-40ca-9954-164c70ed180f'::uuid,
    'PENDING',
    (SELECT now_ts FROM _seed_clock) - interval '2 days',
    (SELECT now_ts FROM _seed_clock) - interval '2 days'
),
(
    'b3000000-0000-0000-0000-000000000002'::uuid,
    'LAB_REQUEST',
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    'fdc76d3e-0e13-40ca-9954-164c70ed180f'::uuid,
    'REJECTED',
    (SELECT now_ts FROM _seed_clock) - interval '10 days',
    (SELECT now_ts FROM _seed_clock) - interval '9 days'
)
ON CONFLICT (id) DO UPDATE
SET
    record_type = EXCLUDED.record_type,
    patient_id = EXCLUDED.patient_id,
    doctor_id = EXCLUDED.doctor_id,
    status = EXCLUDED.status,
    created_at = EXCLUDED.created_at,
    last_updated = EXCLUDED.last_updated;

INSERT INTO appointment_bills (
    id, amount, insurance_cover_amount, patient_payment_amount, status, type,
    patient_id, confirm_accountant_id, paid_on, created_at, last_updated, version, appointment_id
)
VALUES
(
    '93000000-0000-0000-0000-000000000001'::uuid,
    75.00,
    50.00,
    25.00,
    'UNPAID',
    'QUICK_CHECK_FEE',
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    NULL,
    NULL,
    (SELECT now_ts FROM _seed_clock) - interval '20 days' + interval '9 hours' + interval '5 minutes',
    (SELECT now_ts FROM _seed_clock) - interval '20 days' + interval '9 hours' + interval '5 minutes',
    0,
    'a3000000-0000-0000-0000-000000000001'::uuid
),
(
    '93000000-0000-0000-0000-000000000002'::uuid,
    150.00,
    120.00,
    30.00,
    'PAID',
    'MID_CHECK_FEE',
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    '2c354b57-9111-4806-a365-10ba3337d2fc'::uuid,
    (SELECT now_ts FROM _seed_clock) - interval '17 days',
    (SELECT now_ts FROM _seed_clock) - interval '18 days' + interval '10 hours' + interval '8 minutes',
    (SELECT now_ts FROM _seed_clock) - interval '17 days',
    0,
    'a3000000-0000-0000-0000-000000000002'::uuid
),
(
    '93000000-0000-0000-0000-000000000003'::uuid,
    250.00,
    200.00,
    50.00,
    'UNPAID',
    'LONG_CHECK_FEE',
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    NULL,
    NULL,
    (SELECT now_ts FROM _seed_clock) - interval '16 days' + interval '11 hours' + interval '6 minutes',
    (SELECT now_ts FROM _seed_clock) - interval '16 days' + interval '11 hours' + interval '6 minutes',
    0,
    'a3000000-0000-0000-0000-000000000003'::uuid
),
(
    '93000000-0000-0000-0000-000000000004'::uuid,
    25.00,
    0.00,
    25.00,
    'UNPAID',
    'QUICK_CHECK_CANCELLATION_FEE',
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    NULL,
    NULL,
    (SELECT now_ts FROM _seed_clock) - interval '1 day',
    (SELECT now_ts FROM _seed_clock) - interval '1 day',
    0,
    'a3000000-0000-0000-0000-000000000004'::uuid
),
(
    '93000000-0000-0000-0000-000000000005'::uuid,
    25.00,
    0.00,
    25.00,
    'UNPAID',
    'QUICK_CHECK_CANCELLATION_FEE',
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    NULL,
    NULL,
    (SELECT now_ts FROM _seed_clock) - interval '8 days' + interval '9 hours' + interval '20 minutes',
    (SELECT now_ts FROM _seed_clock) - interval '8 days' + interval '9 hours' + interval '20 minutes',
    0,
    'a3000000-0000-0000-0000-000000000005'::uuid
),
(
    '93000000-0000-0000-0000-000000000006'::uuid,
    75.00,
    75.00,
    0.00,
    'PAID',
    'QUICK_CHECK_FEE',
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    '2c354b57-9111-4806-a365-10ba3337d2fc'::uuid,
    (SELECT now_ts FROM _seed_clock) - interval '5 days',
    (SELECT now_ts FROM _seed_clock) - interval '6 days' + interval '14 hours' + interval '3 minutes',
    (SELECT now_ts FROM _seed_clock) - interval '5 days',
    0,
    'a3000000-0000-0000-0000-000000000006'::uuid
)
ON CONFLICT (id) DO UPDATE
SET
    amount = EXCLUDED.amount,
    insurance_cover_amount = EXCLUDED.insurance_cover_amount,
    patient_payment_amount = EXCLUDED.patient_payment_amount,
    status = EXCLUDED.status,
    type = EXCLUDED.type,
    patient_id = EXCLUDED.patient_id,
    confirm_accountant_id = EXCLUDED.confirm_accountant_id,
    paid_on = EXCLUDED.paid_on,
    created_at = EXCLUDED.created_at,
    last_updated = EXCLUDED.last_updated,
    version = EXCLUDED.version,
    appointment_id = EXCLUDED.appointment_id;

INSERT INTO lab_bills (
    id, amount, insurance_cover_amount, patient_payment_amount, status, type,
    patient_id, confirm_accountant_id, paid_on, created_at, last_updated, version, lab_test_id
)
VALUES
(
    '94000000-0000-0000-0000-000000000001'::uuid,
    45.00,
    45.00,
    0.00,
    'PAID',
    'LAB_TEST_FEE',
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    '2c354b57-9111-4806-a365-10ba3337d2fc'::uuid,
    (SELECT now_ts FROM _seed_clock) - interval '17 days',
    (SELECT now_ts FROM _seed_clock) - interval '17 days' + interval '10 hours',
    (SELECT now_ts FROM _seed_clock) - interval '17 days',
    0,
    'e3000000-0000-0000-0000-000000000001'::uuid
),
(
    '94000000-0000-0000-0000-000000000002'::uuid,
    80.00,
    80.00,
    0.00,
    'PAID',
    'LAB_TEST_FEE',
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    '2c354b57-9111-4806-a365-10ba3337d2fc'::uuid,
    (SELECT now_ts FROM _seed_clock) - interval '17 days',
    (SELECT now_ts FROM _seed_clock) - interval '17 days' + interval '11 hours',
    (SELECT now_ts FROM _seed_clock) - interval '17 days',
    0,
    'e3000000-0000-0000-0000-000000000002'::uuid
),
(
    '94000000-0000-0000-0000-000000000003'::uuid,
    55.00,
    40.00,
    15.00,
    'UNPAID',
    'LAB_TEST_FEE',
    '010e2010-95c7-460d-a39a-74c540158661'::uuid,
    NULL,
    NULL,
    (SELECT now_ts FROM _seed_clock) - interval '5 days' + interval '9 hours',
    (SELECT now_ts FROM _seed_clock) - interval '5 days' + interval '9 hours',
    0,
    'e3000000-0000-0000-0000-000000000003'::uuid
)
ON CONFLICT (id) DO UPDATE
SET
    amount = EXCLUDED.amount,
    insurance_cover_amount = EXCLUDED.insurance_cover_amount,
    patient_payment_amount = EXCLUDED.patient_payment_amount,
    status = EXCLUDED.status,
    type = EXCLUDED.type,
    patient_id = EXCLUDED.patient_id,
    confirm_accountant_id = EXCLUDED.confirm_accountant_id,
    paid_on = EXCLUDED.paid_on,
    created_at = EXCLUDED.created_at,
    last_updated = EXCLUDED.last_updated,
    version = EXCLUDED.version,
    lab_test_id = EXCLUDED.lab_test_id;

COMMIT;
