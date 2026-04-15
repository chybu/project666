package com.project666.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project666.backend.domain.entity.RoleEnum;
import com.project666.backend.domain.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>{
    Optional<User> findByIdAndRole(UUID userId, RoleEnum role);
    Optional<User> findByIdAndDeletedFalse(UUID userId);
    Optional<User> findByIdAndRoleAndDeletedFalse(UUID userId, RoleEnum role);
    List<User> findAllByRoleAndDeletedFalse(RoleEnum role);
    boolean existsByIdAndDeletedFalse(UUID userId);
    List<User> findByRoleAndDeletedFalse(RoleEnum role);
}
