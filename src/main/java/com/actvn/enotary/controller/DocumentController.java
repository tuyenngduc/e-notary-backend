package com.actvn.enotary.controller;

import com.actvn.enotary.entity.Document;
import com.actvn.enotary.entity.NotaryRequest;
import com.actvn.enotary.security.CustomUserDetails;
import com.actvn.enotary.service.NotaryRequestService;
import com.actvn.enotary.repository.DocumentRepository;
import com.actvn.enotary.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.PathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentRepository documentRepository;
    private final NotaryRequestService notaryRequestService;

    @GetMapping("/{id}")
    public ResponseEntity<?> downloadDocument(
            Authentication authentication,
            @PathVariable("id") UUID id,
            HttpServletRequest request
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return ResponseEntity.status(401).build();
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();

        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy tài liệu", org.springframework.http.HttpStatus.NOT_FOUND));

        NotaryRequest req = doc.getRequest();
        boolean isOwner = req.getClient() != null && req.getClient().getEmail().equals(email);
        boolean isAssignedNotary = req.getNotary() != null && req.getNotary().getEmail().equals(email);
        boolean isAdmin = userDetails.getRole() != null && userDetails.getRole().name().equals("ADMIN");

        if (!isOwner && !isAssignedNotary && !isAdmin) {
            return ResponseEntity.status(403).build();
        }

        try {
            Path projectRoot = notaryRequestService.getProjectRootPublic();
            Path file = projectRoot.resolve(Path.of(doc.getFilePath())).normalize();
            if (!Files.exists(file)) {
                throw new AppException("File not found on disk", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
            }

            String contentType = Files.probeContentType(file);
            if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

            PathResource resource = new PathResource(file);
            String disposition = "attachment; filename=\"" + file.getFileName().toString() + "\"";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AppException("Lỗi khi trả file", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

