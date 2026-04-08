package com.project666.frontend.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.project666.backend.domain.entity.User;

@Service
public class KeycloakService {

    @Value("${keycloak.base-url}")
    private String KEYCLOAK_BASE;

    @Value("${keycloak.realm}")
    private String REALM;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.account-url}")
    private String accountUrl;

    @Value("${keycloak.password-url}")
    private String passwordUrl;

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

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("client_id", "frontend-client");
    body.add("grant_type", "password");
    body.add("username", username);
    body.add("password", password);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

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

        String url = KEYCLOAK_BASE + "/realms/" + REALM + "/protocol/openid-connect/token";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        return (String) response.getBody().get("access_token");
    }

    public String getAccountRedirect() {
        return "redirect:" + accountUrl;
}

    public String getPasswordRedirect() {
        return "redirect:" + passwordUrl;
}
}