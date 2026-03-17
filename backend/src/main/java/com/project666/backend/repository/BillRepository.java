package com.project666.backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project666.backend.domain.entity.AppointmentBill;

@Repository
public interface BillRepository extends JpaRepository<AppointmentBill, UUID>{

}
