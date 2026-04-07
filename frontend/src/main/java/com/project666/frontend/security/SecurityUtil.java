package com.project666.frontend.security;

import java.util.NoSuchElementException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import com.project666.backend.domain.entity.RoleEnum;

public final class SecurityUtil {

    public static RoleEnum getUserRole(Authentication authentication){
        String role = authentication
            .getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .filter(auth -> auth
                .startsWith("ROLE_")
            )
            .findFirst()
            .orElseThrow(NoSuchElementException::new);
                
        role = role.replace("ROLE_", "");
        try {
            return RoleEnum.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("%s cannot be mapped to enum", role));
        }
    }

    public static String getDashboardHomePath(Authentication authentication){
        RoleEnum role = getUserRole(authentication);
        return switch (role){
            case PATIENT -> "/patient/dashboard/home";
            case DOCTOR -> "/doctor/dashboard/home";
            case RECEPTIONIST -> "/receptionist/dashboard/home";
            case NURSE -> "/nurse/dashboard/home";
            case LAB_TECHNICIAN -> "/labtechnician/dashboard/home";
            case ACCOUNTANT -> "/accountant/dashboard/home";
        };
    }
}
