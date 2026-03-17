package com.project666.frontend.util;

import java.util.UUID;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public final class OidcUserUtil {
    public static UUID getUserId(OidcUser oidcUser){
        return UUID.fromString(oidcUser.getSubject());
    }
}
