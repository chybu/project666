package com.project666.backend.domain.entity;

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
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, updatable = false)
    private String email;

    @Column(name = "name", nullable = false, updatable = false)
    private String name;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, updatable = false)
    private RoleEnum role;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    // belongs to receptionist or patient
    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL)
    private List<Appointment> createdAppointments = new ArrayList<>();

    // belongs to receptionist or patient
    @OneToMany(mappedBy = "canceller", cascade = CascadeType.ALL)
    private List<Appointment> cancelledAppointments = new ArrayList<>();

    // belongs to receptionist
    @OneToMany(mappedBy = "confirmReceptionist", cascade = CascadeType.ALL)
    private List<Appointment> confirmedAppointments = new ArrayList<>();

    // belong to doctor
    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL)
    private List<Appointment> patientAppointments = new ArrayList<>();

    // belong to patient
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL)
    private List<Appointment> doctorAppointments = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL)
    private List<AppointmentBill> appointmentBills = new ArrayList<>();

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
        User other = (User) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    public String getFullName(){
        return firstName + " " + lastName;
    }
}
