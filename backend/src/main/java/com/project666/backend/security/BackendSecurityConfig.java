package com.project666.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class BackendSecurityConfig {


    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(
        HttpSecurity http,
        JwtAuthenticationConverter jwtAuthenticationConverter
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
            .jwt(jwt -> jwt
                .jwtAuthenticationConverter(jwtAuthenticationConverter) // Map Keycloak realm roles to Spring Security roles
            )
        );
        return http.build();
    }
}
