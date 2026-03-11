package com.hospital.portal.config;

import com.hospital.portal.service.KeycloakOidcUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final KeycloakOidcUserService keycloakOidcUserService;

    public SecurityConfig(KeycloakOidcUserService keycloakOidcUserService) {
    this.keycloakOidcUserService = keycloakOidcUserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/").permitAll()
                .requestMatchers("/doctor").hasAuthority("ROLE_DOCTOR")
                .requestMatchers("/patient").hasAuthority("ROLE_PATIENT")
                .requestMatchers("/receptionist").hasAuthority("ROLE_RECEPTIONIST")
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(keycloakOidcUserService)
                )
                .successHandler(roleBasedSuccessHandler())
            )
            .logout(logout -> logout
                .logoutSuccessHandler(oidcLogoutSuccessHandler())
            );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return (request, response, authentication) -> {
            OidcUser user = (OidcUser) authentication.getPrincipal();

            String redirect;
            if (user.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_DOCTOR"))) {
                redirect = "/doctor";
            } else if (user.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PATIENT"))) {
                redirect = "/patient";
            } else if (user.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_RECEPTIONIST"))) {
                redirect = "/receptionist";
            } else {
                redirect = "/?error=no_role";
            }

            response.sendRedirect(redirect);
        };
    }

    @Bean
    public LogoutSuccessHandler oidcLogoutSuccessHandler() {
        return (request, response, authentication) -> {
            String idToken = "";
            if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
                idToken = oidcUser.getIdToken().getTokenValue();
            }

            String logoutUrl = "http://localhost:9090/realms/patient-portal/protocol/openid-connect/logout"
                + "?post_logout_redirect_uri=http://localhost:8080/"
                + "&id_token_hint=" + idToken;

            response.sendRedirect(logoutUrl);
        };
    }
    @Bean
    public JwtDecoderFactory<ClientRegistration> idTokenDecoderFactory() {
        OidcIdTokenDecoderFactory factory = new OidcIdTokenDecoderFactory();
        factory.setJwtValidatorFactory(clientRegistration ->
            jwt -> org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success()
        );
        return factory;
    }
}
