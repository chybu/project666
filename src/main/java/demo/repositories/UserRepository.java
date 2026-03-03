package demo.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import demo.domains.entities.RoleEnum;
import demo.domains.entities.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>{
    Optional<User> findByIdAndRole(UUID userId, RoleEnum role);
}
