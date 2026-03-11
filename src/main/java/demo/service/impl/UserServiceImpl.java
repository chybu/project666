package demo.service.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import demo.domain.entities.RoleEnum;
import demo.domain.entities.User;
import demo.exception.RoleNotFoundException;
import demo.exception.UnknowRoleException;
import demo.repository.UserRepository;
import demo.service.UserService;
import demo.util.JwtUtil;
import demo.util.OidcUserUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    final UserRepository userRepository;

    private final String KEYCLOAK_PATIENT_ROLE = "ROLE_PATIENT";
    private final String KEYCLOAK_RECEPTIONIST_ROLE = "ROLE_RECEPTIONIST";
    private final String KEYCLOAK_DOCTOR_ROLE = "ROLE_DOCTOR";

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
        RoleEnum role = getUserRole(realmAccess);
        user.setRole(role);

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // User already exists
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Ensures that even if the catch block is triggered, main API can still proceed because its transaction is separate.
    public void provisionUser(OidcUser oidcUser) {
        UUID keycloakId = OidcUserUtil.getUserId(oidcUser);

        if (userRepository.existsById(keycloakId)) {
            return;
        }

        User user = new User();
        user.setId(keycloakId);
        user.setEmail(oidcUser.getClaimAsString("email"));
        user.setName(oidcUser.getClaimAsString("preferred_username"));
        user.setFirstName(oidcUser.getClaimAsString("given_name"));
        user.setLastName(oidcUser.getClaimAsString("family_name"));
        
        Map<String, Object> realmAccess = oidcUser.getClaim("realm_access");
        RoleEnum role = getUserRole(realmAccess);
        user.setRole(role);

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // User already exists
        }
    }

    private RoleEnum getUserRole(Map<String, Object> realmAccess){

        if(realmAccess==null || !realmAccess.containsKey("roles")){
            throw new RoleNotFoundException();
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");

        if (roles.contains(KEYCLOAK_PATIENT_ROLE)){
            return RoleEnum.PATIENT;
        }
        else if(roles.contains(KEYCLOAK_RECEPTIONIST_ROLE)){
            return RoleEnum.RECEPTIONIST;
        }
        else if(roles.contains(KEYCLOAK_DOCTOR_ROLE)){
            return RoleEnum.DOCTOR;
        }
        else{
            throw new UnknowRoleException(roles.toString());
        }
    }
}
