package com.project666.frontend.service.impl;

import java.util.Map;
import java.util.UUID;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.project666.backend.domain.entity.RoleEnum;
import com.project666.backend.domain.entity.User;
import com.project666.backend.repository.UserRepository;

import com.project666.frontend.service.FrontenndUserService;
import com.project666.frontend.util.OidcUserUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FrontendUserServiceImpl implements FrontenndUserService{

    final UserRepository userRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Ensures that even if the catch block is triggered, main API can still proceed because its transaction is separate.
    public void provisionUser(OidcUser oidcUser) {
        UUID keycloakId = OidcUserUtil.getUserId(oidcUser);

        User user = userRepository.findById(keycloakId)
            .orElse(new User());

        user.setId(keycloakId);
        user.setEmail(oidcUser.getClaimAsString("email"));
        user.setName(oidcUser.getClaimAsString("preferred_username"));
        user.setFirstName(oidcUser.getClaimAsString("given_name"));
        user.setLastName(oidcUser.getClaimAsString("family_name"));
        
        Map<String, Object> realmAccess = oidcUser.getClaim("realm_access");
        RoleEnum role = RoleEnum.getUserRole(realmAccess);
        user.setRole(role);

        userRepository.saveAndFlush(user);
    }
}