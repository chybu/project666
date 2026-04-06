package com.project666.backend.domain.entity;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public enum RoleEnum {
    PATIENT,
    RECEPTIONIST,
    DOCTOR,
    NURSE,
    LAB_TECHNICIAN;

    private static final String KEYCLOAK_PATIENT_ROLE = "ROLE_PATIENT";
    private static final String KEYCLOAK_RECEPTIONIST_ROLE = "ROLE_RECEPTIONIST";
    private static final String KEYCLOAK_DOCTOR_ROLE = "ROLE_DOCTOR";
    private static final String KEYCLOAK_NURSE_ROLE = "ROLE_NURSE";
    private static final String KEYCLOAK_LAB_TECHNICIAN_ROLE = "ROLE_LABTECHNICIAN";


    public static RoleEnum getUserRole(Map<String, Object> realmAccess){

        if(realmAccess==null || !realmAccess.containsKey("roles")){
            throw new NoSuchElementException();
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
        else if(roles.contains(KEYCLOAK_LAB_TECHNICIAN_ROLE)){
            return RoleEnum.LAB_TECHNICIAN;
        }
        else{
            throw new IllegalArgumentException(String.format("%s role is not known", roles.toString()));
        }
    }
}
