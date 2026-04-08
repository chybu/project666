package com.project666.backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.project666.backend.domain.entity.AppointmentBill;

@Repository
public interface AppointmentBillRepository extends 
    JpaRepository<AppointmentBill, UUID>,
    JpaSpecificationExecutor<AppointmentBill>
    {

}
