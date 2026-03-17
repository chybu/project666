package com.project666.backend.service.impl;

import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.project666.backend.domain.entity.RoleEnum;
import com.project666.backend.domain.entity.User;
import com.project666.backend.repository.UserRepository;
import com.project666.backend.service.BackendUserService;
import com.project666.backend.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackendUserServiceImpl implements BackendUserService{

    final UserRepository userRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Ensures that even if the catch block is triggered, main API can still proceed because its transaction is separate.
    public void provisionUser(Jwt jwt) {
        UUID keycloakId = JwtUtil.getUserId(jwt);

        if (userRepository.existsById(keycloakId)) {
            return;
        }

        User user = new User();
        user.setId(keycloakId);
        user.setEmail(jwt.getClaimAsString("email"));
        user.setName(jwt.getClaimAsString("preferred_username"));
        user.setFirstName(jwt.getClaimAsString("given_name"));
        user.setLastName(jwt.getClaimAsString("family_name"));

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        RoleEnum role = RoleEnum.getUserRole(realmAccess);
        user.setRole(role);

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // User already exists
        }
    }
}
