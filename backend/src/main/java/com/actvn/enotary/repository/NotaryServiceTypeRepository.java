package com.actvn.enotary.repository;

import com.actvn.enotary.entity.NotaryServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;

@Repository
public interface NotaryServiceTypeRepository extends JpaRepository<NotaryServiceType, UUID> {
    Optional<NotaryServiceType> findByServiceCode(String serviceCode);
}
