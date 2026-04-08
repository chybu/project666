package com.project666.frontend.service;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Keeps the frontend's local user record aligned with the authenticated OIDC user.
 */
public interface FrontenndUserService {
    /**
     * Creates or updates the local user record from OIDC claims after login.
     */
    void provisionUser(OidcUser oidcUser);
}
