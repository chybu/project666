package com.project666.backend.service;

import org.springframework.security.oauth2.jwt.Jwt;

public interface BackendUserService {
    void provisionUser(Jwt jwt);
}
