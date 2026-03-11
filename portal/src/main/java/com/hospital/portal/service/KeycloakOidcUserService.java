package com.hospital.portal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

@Service
public class KeycloakOidcUserService extends OidcUserService {

    private final ObjectMapper mapper = new ObjectMapper();

    public KeycloakOidcUserService() {
        setAccessibleScopes(Set.of());
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser user = super.loadUser(userRequest);

        Set<GrantedAuthority> authorities = new HashSet<>(user.getAuthorities());
        authorities.addAll(extractRealmRoles(userRequest.getAccessToken().getTokenValue()));

        return new DefaultOidcUser(authorities, user.getIdToken(), "preferred_username");
    }

    private Set<GrantedAuthority> extractRealmRoles(String accessToken) {
        Set<GrantedAuthority> roles = new HashSet<>();
        try {
            String[] parts = accessToken.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode claims = mapper.readTree(payload);

            JsonNode realmRoles = claims.path("realm_access").path("roles");
            if (realmRoles.isArray()) {
                for (JsonNode role : realmRoles) {
                    roles.add(new SimpleGrantedAuthority(role.asText()));
                }
            }
        } catch (Exception e) {
            // return empty on failure
        }
        return roles;
    }
}