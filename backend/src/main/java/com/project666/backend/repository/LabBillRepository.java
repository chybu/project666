package com.project666.backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project666.backend.domain.entity.LabBill;

@Repository
public interface LabBillRepository extends JpaRepository<LabBill, UUID>{

}
