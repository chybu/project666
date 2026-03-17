package com.project666.backend.domain.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// @Entity
// @Table(name = "lab_bills")
// @Getter
// @Setter
// @NoArgsConstructor
// @AllArgsConstructor
public class LabBill extends BaseBill{

    // REMEMBER TO ADD @ONETOMANY IN USER
    
    // @ManyToOne
    // @JoinColumn(name = "lab_response_id", nullable = false, updatable = false)
    // private List<LabResponse> labResponses = new ArrayList<>();
}
