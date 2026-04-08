package com.project666.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;


import jakarta.servlet.http.HttpServletResponse;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class BackendSecurityConfig {
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(
        HttpSecurity http,
        JwtAuthenticationConverter jwtAuthenticationConverter,
        AuthenticationEntryPoint apiAuthenticationEntryPoint,
        AccessDeniedHandler apiAccessDeniedHandler
    ){

        http
        .securityMatcher("/api/**") // Only apply these rules for /api/** 
        .authorizeHttpRequests(auth -> auth
            .anyRequest().authenticated() // all API requests require auth
        )
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // No HTTP session is created because this is a REST API
        .csrf(csrf -> csrf
            .disable()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            // This is used when the user is not authenticated.
            .authenticationEntryPoint(apiAuthenticationEntryPoint)
            // This is used when the user is authenticated, but is not allowed. (Maybe cannot map)
            .accessDeniedHandler(apiAccessDeniedHandler)
            .jwt(jwt -> jwt
                .jwtAuthenticationConverter(jwtAuthenticationConverter) // Map Keycloak realm roles to Spring Security roles
            )
        );
        return http.build();
    }
    
    @Bean
    public AuthenticationEntryPoint apiAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            // response.getWriter().write("{\"errorMessage\":\"Authentication required\"}");
            // TODO: REMEMBER TO COMMENT THIS OUT IN PRODUCTION
            response.getWriter().write(
                "{\"errorMessage\":\"" + authException.getMessage().replace("\"", "\\\"") + "\"}"
            );
        };
    }

    @Bean
    public AccessDeniedHandler apiAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            // response.getWriter().write("{\"errorMessage\":\"Access denied\"}");
            // TODO: REMEMBER TO COMMENT THIS OUT IN PRODUCTION
                response.getWriter().write(
                "{\"errorMessage\":\"" + accessDeniedException.getMessage().replace("\"", "\\\"") + "\"}"
            );
        };
    }

}
