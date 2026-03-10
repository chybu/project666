package demo.util;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

import demo.domain.entities.RoleEnum;
import demo.exception.UnknowRoleException;

public final class JwtUtil {
    public static UUID getUserId(Jwt jwt){
        return UUID.fromString(jwt.getSubject());
    }

    public static RoleEnum getRole(Jwt jwt){
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if(realmAccess==null || !realmAccess.containsKey("roles")){
            return null;
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");

        String role = roles
            .stream()
            .filter(r -> r.startsWith("ROLE_"))
            .findFirst()
            .orElse(null);

        role = role.replace("ROLE_", "");

        try {
            return RoleEnum.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new UnknowRoleException();
        }
        
    }
}
