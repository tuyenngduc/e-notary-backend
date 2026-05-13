package com.actvn.enotary.repository;

import com.actvn.enotary.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Optional<Appointment> findByRequestRequestId(UUID requestId);

    boolean existsByRequestRequestId(UUID requestId);

    java.util.List<Appointment> findByRequestNotaryUserIdOrderByScheduledTimeAsc(UUID notaryId);
}

