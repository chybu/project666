package demo.services;

import org.springframework.security.oauth2.jwt.Jwt;

public interface UserService {
    void provisionUser(Jwt jwt);
}
