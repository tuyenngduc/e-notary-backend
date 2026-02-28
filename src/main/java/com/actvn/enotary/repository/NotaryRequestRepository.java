package com.actvn.enotary.repository;

import com.actvn.enotary.entity.NotaryRequest;
import com.actvn.enotary.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotaryRequestRepository extends JpaRepository<NotaryRequest, UUID> {
    List<NotaryRequest> findByClientUserId(UUID userId);

    Page<NotaryRequest> findByStatus(RequestStatus status, Pageable pageable);

    Page<NotaryRequest> findByNotaryUserIdAndStatus(UUID notaryUserId, RequestStatus status, Pageable pageable);
}
