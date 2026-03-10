package demo.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import demo.domain.entities.RoleEnum;
import demo.domain.entities.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>{
    Optional<User> findByIdAndRole(UUID userId, RoleEnum role);
}
