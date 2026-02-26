-- Create a dedicated Keycloak app user (non-superuser)
CREATE USER keycloak_app WITH PASSWORD 'password';

-- Give it access only to the keycloak database
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak_app;

-- Grant schema privileges
\c keycloak;
GRANT ALL ON SCHEMA public TO keycloak_app;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO keycloak_app;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO keycloak_app;