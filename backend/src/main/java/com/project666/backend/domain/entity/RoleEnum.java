package com.project666.backend.domain.entity;

import java.util.List;
import java.util.Map;

import com.project666.backend.exception.RoleNotFoundException;
import com.project666.backend.exception.UnknowRoleException;

public enum RoleEnum {
    PATIENT,
    RECEPTIONIST,
    DOCTOR,
    NURSE;

    private static final String KEYCLOAK_PATIENT_ROLE = "ROLE_PATIENT";
    private static final String KEYCLOAK_RECEPTIONIST_ROLE = "ROLE_RECEPTIONIST";
    private static final String KEYCLOAK_DOCTOR_ROLE = "ROLE_DOCTOR";
    private static final String KEYCLOAK_NURSE_ROLE = "ROLE_NURSE";


    public static RoleEnum getUserRole(Map<String, Object> realmAccess){

        if(realmAccess==null || !realmAccess.containsKey("roles")){
            throw new RoleNotFoundException();
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");

        if (roles.contains(KEYCLOAK_PATIENT_ROLE)){
            return RoleEnum.PATIENT;
        }
        else if(roles.contains(KEYCLOAK_RECEPTIONIST_ROLE)){
            return RoleEnum.RECEPTIONIST;
        }
        else if(roles.contains(KEYCLOAK_DOCTOR_ROLE)){
            return RoleEnum.DOCTOR;
        }
        else if(roles.contains(KEYCLOAK_NURSE_ROLE)){
            return RoleEnum.NURSE;
        }
        else{
            throw new UnknowRoleException(roles.toString());
        }
    }
}
