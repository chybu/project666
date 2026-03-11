package demo.service;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;

public interface UserService {
    void provisionUser(Jwt jwt);

    void provisionUser(OidcUser oidcUser);
}
