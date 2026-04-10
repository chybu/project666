package com.project666.frontend.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.project666.frontend.service.KeycloakService;
import com.project666.frontend.util.OidcUserUtil;
import com.project666.backend.domain.entity.User;
import com.project666.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/nurse")
@PreAuthorize("hasRole('NURSE')")
public class NurseController {

    private final UserRepository userRepository;
    private final KeycloakService keycloakService;


    @GetMapping("/dashboard/home")
    public String home(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient
    ) {
        User user = requireActiveUser(oidcUser);

        keycloakService.syncUser(authorizedClient, user);
        userRepository.save(user);

        return "nurse/dashboard/home";
    }

    @GetMapping("/dashboard/appointments")
    public String appointments() {
        return "nurse/dashboard/appointments";
    }

    @GetMapping("/dashboard/notifications")
    public String notifications() {
        return "nurse/dashboard/notifications";
    }

    @GetMapping("/dashboard/profile")
    public String profile(
        @AuthenticationPrincipal OidcUser oidcUser,
        @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
        Model model
    ){
        User user = requireActiveUser(oidcUser);

        keycloakService.syncUser(authorizedClient, user);
        userRepository.save(user);

        model.addAttribute("user", user);

        return "nurse/dashboard/profile";
    }

    @PostMapping("/delete-account")
    public String deleteAccount(
        @AuthenticationPrincipal OidcUser oidcUser
    ){
        User user = requireActiveUser(oidcUser);

        keycloakService.deleteUser(user.getId());
        user.setDeleted(true);
        userRepository.save(user);

        return "redirect:/logout";
    }

    @GetMapping("/profile/keycloak")
    public String redirectToKeycloakAccount() {
        return keycloakService.getAccountRedirect();
    }

    @GetMapping("/profile/keycloak-password")
    public String redirectToKeycloakPassword() {
        return keycloakService.getPasswordRedirect();
    }

    private User requireActiveUser(OidcUser oidcUser) {
        UUID userId = OidcUserUtil.getUserId(oidcUser);
        return userRepository.findByIdAndDeletedFalse(userId)
            .orElseThrow();
    }
}
