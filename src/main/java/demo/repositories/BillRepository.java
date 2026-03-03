package demo.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import demo.domains.entities.AppointmentBill;

@Repository
public interface BillRepository extends JpaRepository<AppointmentBill, UUID>{

}
