package com.project666.backend.service;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Keeps backend user records in sync with authenticated Keycloak users.
 */
public interface BackendUserService {
    /**
     * Creates the local user record from JWT claims when it does not already exist.
     */
    void provisionUser(Jwt jwt);
}
