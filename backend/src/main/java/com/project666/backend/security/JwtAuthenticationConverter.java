package com.project666.backend.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.project666.backend.service.BackendUserService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken>{

    private final BackendUserService userService;

    @Override
    public JwtAuthenticationToken convert(Jwt source) {
        try {
            userService.provisionUser(source);
            Collection<GrantedAuthority> authorities = extractAuthorities(source);
            return new JwtAuthenticationToken(source, authorities);
        } catch (OAuth2AuthenticationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            OAuth2Error error = new OAuth2Error(
                "invalid_token",
                ex.getMessage(),
                null
            );
            throw new OAuth2AuthenticationException(error, ex.getMessage(), ex);
        }
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
