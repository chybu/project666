package com.project666.frontend.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity
public class FrontendSecurityConfig {

    private final ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public SecurityFilterChain frontendFilterChain(
        HttpSecurity http
    ){
        http
        .securityMatcher("/**")
        .authorizeHttpRequests(auth -> auth
            // Static assets (no authentication)
            .requestMatchers("/", "/login", "/landingPage/**", "/images/**", "/css/**").permitAll()
            .anyRequest().authenticated() // Internal Page
        )
        .oauth2Login(oauth2 -> oauth2
            .defaultSuccessUrl("/callback", true)
            .userInfoEndpoint(userInfo -> userInfo
                .userAuthoritiesMapper(userAuthoritiesMapper())
            )
        )
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
        )
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessHandler(oidcLogoutSuccessHandler())
        );
        
        return http.build();
    }

    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapper(){
        return (authorities) -> {
            
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
            authorities.forEach(authority -> {
                mappedAuthorities.add(authority);

                if (authority instanceof OidcUserAuthority oidcUserAuthority){
                    Map<String, Object> attributes = oidcUserAuthority.getAttributes();
                    if (attributes.containsKey("realm_access")){
                        @SuppressWarnings("unchecked")
                        Map<String, Object> realmAccess = (Map<String, Object>)attributes.get("realm_access");
                        @SuppressWarnings("unchecked")
                        Collection<String> roles = (Collection<String>)realmAccess.get("roles");
                        roles.forEach(role -> {
                            if (role.startsWith("ROLE_")){
                                mappedAuthorities.add(new SimpleGrantedAuthority(role));
                            }
                        });
                    }
                }

            });

            return mappedAuthorities;
        };
    }
    
    
    // Custom logout handler for OAuth2 logout with Keycloak
    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler logoutSuccessHandler =
            new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        logoutSuccessHandler.setPostLogoutRedirectUri("http://localhost:8080"); // Redirect after logout
        return logoutSuccessHandler;
    }
}
