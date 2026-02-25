package com.actvn.enotary.controller;

import com.actvn.enotary.dto.request.NotaryRequestCreateRequest;
import com.actvn.enotary.dto.response.DocumentResponse;
import com.actvn.enotary.dto.response.NotaryRequestResponse;
import com.actvn.enotary.entity.Document;
import com.actvn.enotary.entity.NotaryRequest;
import com.actvn.enotary.enums.DocType;
import com.actvn.enotary.security.CustomUserDetails;
import com.actvn.enotary.service.NotaryRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class NotaryRequestController {

    private final NotaryRequestService notaryRequestService;

    @PostMapping
    public ResponseEntity<NotaryRequestResponse> createRequest(
            Authentication authentication,
            @Valid @RequestBody NotaryRequestCreateRequest request) {

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return ResponseEntity.status(401).build();
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();

        NotaryRequest created = notaryRequestService.createRequest(email, request);
        URI location = URI.create("/api/requests/" + created.getRequestId());
        return ResponseEntity.created(location).body(NotaryRequestResponse.fromEntity(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotaryRequestResponse> getRequest(
            Authentication authentication,
            @PathVariable("id") UUID id) {

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return ResponseEntity.status(401).build();
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();

        NotaryRequest r = notaryRequestService.getById(id);

        // authorize: allow if user is client owner, or assigned notary, or admin
        var user = userDetails;
        boolean isOwner = r.getClient() != null && r.getClient().getEmail().equals(email);
        boolean isAssignedNotary = r.getNotary() != null && r.getNotary().getEmail().equals(email);
        boolean isAdmin = user.getRole().name().equals("ADMIN");

        if (!isOwner && !isAssignedNotary && !isAdmin) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(NotaryRequestResponse.fromEntity(r));
    }

    @GetMapping("/me")
    public ResponseEntity<List<NotaryRequestResponse>> listMyRequests(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return ResponseEntity.status(401).build();
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getId();

        List<NotaryRequest> list = notaryRequestService.listForClient(userId);
        List<NotaryRequestResponse> resp = list.stream().map(NotaryRequestResponse::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(resp);
    }

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            Authentication authentication,
            @PathVariable("id") UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("docType") DocType docType
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return ResponseEntity.status(401).build();
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();

        Document doc = notaryRequestService.uploadDocument(id, email, file, docType);
        URI location = URI.create("/api/documents/" + doc.getDocumentId());
        var resp = DocumentResponse.fromEntity(doc);
        // compute absolute path from project root + relative filePath (doc.filePath stores relative path)
        try {
            var projectRoot = notaryRequestService.getProjectRootPublic();
            if (resp.getFilePath() != null) {
                String abs = projectRoot.resolve(Path.of(resp.getFilePath())).toAbsolutePath().toString();
                resp.setAbsolutePath(abs);
            }
        } catch (Exception ignored) {
        }
        return ResponseEntity.created(location).body(resp);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<NotaryRequestResponse> cancelRequest(
            Authentication authentication,
            @PathVariable("id") UUID id) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return ResponseEntity.status(401).build();
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();

        NotaryRequest updated = notaryRequestService.cancelRequest(id, email);
        return ResponseEntity.ok(NotaryRequestResponse.fromEntity(updated));
    }
}
