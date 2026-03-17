package com.project666.frontend.service;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public interface FrontenndUserService {
    void provisionUser(OidcUser oidcUser);
}
