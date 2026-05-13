package com.actvn.enotary.repository;

import com.actvn.enotary.entity.Document;
import com.actvn.enotary.enums.DocType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
	@Query("select distinct d.docType from Document d where d.request.requestId = :requestId")
	List<DocType> findDocTypesByRequestId(@Param("requestId") UUID requestId);

	@Query("select d from Document d where d.request.requestId = :requestId order by d.createdAt desc")
	List<Document> findByRequest_RequestId(@Param("requestId") UUID requestId);
}

