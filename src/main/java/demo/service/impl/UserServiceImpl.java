package demo.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import demo.domain.entities.RoleEnum;
import demo.domain.entities.User;
import demo.exception.UnknowRoleException;
import demo.repository.UserRepository;
import demo.service.UserService;
import demo.util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    final UserRepository userRepository;

    private final String KEYCLOAK_PATIENT_ROLE = "ROLE_PATIENT";
    private final String KEYCLOAK_RECEPTIONIST_ROLE = "ROLE_RECEPTIONIST";
    private final String KEYCLOAK_DOCTOR_ROLE = "ROLE_DOCTOR";

    @Override
    @Transactional
    public void provisionUser(Jwt jwt) {
        UUID keycloak_id = JwtUtil.getUserId(jwt);

        User user = new User();
        user.setId(keycloak_id);
        user.setEmail(jwt.getClaimAsString("email"));
        user.setName(jwt.getClaimAsString("preferred_username"));
        user.setFirstName(jwt.getClaimAsString("given_name"));
        user.setLastName(jwt.getClaimAsString("family_name"));
        user.setRole(getUserRole(jwt));

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // User already exists
        }
    }

    private List<String> getRoles(Jwt jwt){
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if(realmAccess==null || !realmAccess.containsKey("roles")){
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");

        return roles;
    }

    private RoleEnum getUserRole(Jwt jwt){
        List<String> roles = getRoles(jwt);
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
