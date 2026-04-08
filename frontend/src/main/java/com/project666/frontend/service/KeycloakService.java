package com.project666.frontend.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.project666.backend.domain.entity.User;

@Service
public class KeycloakService {

    private final String KEYCLOAK_BASE = "http://localhost:9090";
    private final String REALM = "patient-portal";

    public void syncUser(OAuth2AuthorizedClient authorizedClient, User user) {

        String accessToken = authorizedClient.getAccessToken().getTokenValue();

        String url = KEYCLOAK_BASE + "/realms/" + REALM + "/protocol/openid-connect/userinfo";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            Map.class
        );

        Map<String, Object> userInfo = response.getBody();

        user.setFirstName((String) userInfo.get("given_name"));
        user.setLastName((String) userInfo.get("family_name"));
        user.setEmail((String) userInfo.get("email"));
    }

    public boolean verifyPassword(String username, String password) {

        String url = KEYCLOAK_BASE + "/realms/" + REALM + "/protocol/openid-connect/token";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

        String body = "client_id=frontend-client"
            + "&grant_type=password"
            + "&username=" + username
            + "&password=" + password;

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(url, request, Map.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void deleteUser(UUID userId) {

        String adminToken = getAdminToken();

        String url = KEYCLOAK_BASE + "/admin/realms/" + REALM + "/users/" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();

        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }

    private String getAdminToken() {

        String url = KEYCLOAK_BASE + "/realms/master/protocol/openid-connect/token";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

        String body = "client_id=admin-cli"
            + "&grant_type=password"
            + "&username=admin"
            + "&password=password";

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        return (String) response.getBody().get("access_token");
    }
}