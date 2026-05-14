package com.actvn.enotary.repository;

import com.actvn.enotary.entity.ContractTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, UUID> {
    List<ContractTemplate> findByIsActiveTrue();
    List<ContractTemplate> findByServiceTypeIdAndIsActiveTrue(UUID serviceTypeId);
    List<ContractTemplate> findByServiceTypeId(UUID serviceTypeId);
}
