package com.actvn.enotary.controller;

import com.actvn.enotary.entity.Document;
import com.actvn.enotary.entity.User;
import com.actvn.enotary.enums.DocType;
import com.actvn.enotary.security.CustomUserDetails;
import com.actvn.enotary.service.NotaryRequestService;
import com.actvn.enotary.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    private MockMvc mockMvc;
    private NotaryRequestService notaryRequestService;

    private UsernamePasswordAuthenticationToken clientAuth;

    @BeforeEach
    void setUp() {
        notaryRequestService = Mockito.mock(NotaryRequestService.class);
        DocumentRepository documentRepository = Mockito.mock(DocumentRepository.class);

        DocumentController controller = new DocumentController(documentRepository, notaryRequestService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.actvn.enotary.exception.GlobalExceptionHandler())
                .build();

        User clientUser = new User();
        clientUser.setUserId(UUID.randomUUID());
        clientUser.setEmail("client@example.com");
        clientUser.setRole(com.actvn.enotary.enums.Role.CLIENT);

        CustomUserDetails clientDetails = new CustomUserDetails(clientUser);
        clientAuth = new UsernamePasswordAuthenticationToken(clientDetails, null, clientDetails.getAuthorities());
    }

    @Test
    void replaceDocument_returnsOk() throws Exception {
        UUID documentId = UUID.randomUUID();

        Document updated = new Document();
        updated.setDocumentId(documentId);
        updated.setDocType(DocType.DRAFT_CONTRACT);
        updated.setFilePath("uploads/new-file.pdf");
        updated.setFileHash("abc123");

        when(notaryRequestService.replaceDocument(eq(documentId), eq("client@example.com"), any()))
                .thenReturn(updated);

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/documents/{id}", documentId)
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .principal(clientAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(documentId.toString()))
                .andExpect(jsonPath("$.docType").value("DRAFT_CONTRACT"));
    }

    @Test
    void replaceDocument_unauthenticated_returns401() throws Exception {
        UUID documentId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/documents/{id}", documentId)
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isUnauthorized());
    }
}

