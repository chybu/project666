package com.project666.backend.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "prescriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PrescriptionStatusEnum status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false, updatable = false)
    private User doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false, updatable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, updatable = false)
    private Appointment appointment;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_refills", nullable = false)
    private Integer totalRefills;

    @Column(name = "remaining_refills", nullable = false)
    private Integer remainingRefills;

    @Column(name = "refill_interval_days", nullable = false)
    private Integer refillIntervalDays;

    @Column(name = "next_eligible_refill_at", nullable = false)
    private LocalDateTime nextEligibleRefillAt;

    @Column(name = "last_consumed_at")
    private LocalDateTime lastConsumedAt;

    @Column(name = "general_note")
    private String generalNote;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @Version
    private Long version;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL)
    private List<PrescriptionMedicine> medicines = new ArrayList<>();

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Prescription other = (Prescription) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    public void addMedicine(PrescriptionMedicine medicine) {
        medicines.add(medicine);
        medicine.setPrescription(this);
    }
}
