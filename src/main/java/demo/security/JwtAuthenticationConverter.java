package demo.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken>{

    @Override
    public JwtAuthenticationToken convert(Jwt source) {
        Collection<GrantedAuthority> authorities = extractAuthorities(source);
        return new JwtAuthenticationToken(source, authorities);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt source){
        Map<String, Object> realmAccess = source.getClaim("realm_access");

        if(realmAccess==null || !realmAccess.containsKey("roles")){
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");

        return roles.stream()
            .filter(role -> role.startsWith("ROLE_"))
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    }

}
