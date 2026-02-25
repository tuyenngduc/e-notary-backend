package com.actvn.enotary.repository;

import com.actvn.enotary.entity.NotaryRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotaryRequestRepository extends JpaRepository<NotaryRequest, UUID> {
    List<NotaryRequest> findByClientUserId(UUID userId);
}
