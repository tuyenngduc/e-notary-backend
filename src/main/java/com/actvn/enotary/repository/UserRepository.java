package com.actvn.enotary.repository;

import com.actvn.enotary.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByEmail(String email);
}
