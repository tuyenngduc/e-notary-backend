package com.actvn.enotary.controller;

import com.actvn.enotary.dto.request.NotaryRequestCreateRequest;
import com.actvn.enotary.entity.Document;
import com.actvn.enotary.entity.NotaryRequest;
import com.actvn.enotary.entity.User;
import com.actvn.enotary.enums.ContractType;
import com.actvn.enotary.enums.RequestStatus;
import com.actvn.enotary.enums.ServiceType;
import com.actvn.enotary.enums.DocType;
import com.actvn.enotary.security.CustomUserDetails;
import com.actvn.enotary.service.NotaryRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotaryRequestControllerTest {

    private MockMvc mockMvc;
    private NotaryRequestService notaryRequestService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User clientUser;
    private CustomUserDetails clientDetails;
    private UsernamePasswordAuthenticationToken clientAuth;

    @BeforeEach
    void setUp() {
        notaryRequestService = Mockito.mock(NotaryRequestService.class);
        NotaryRequestController controller = new NotaryRequestController(notaryRequestService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        clientUser = new User();
        clientUser.setUserId(UUID.randomUUID());
        clientUser.setEmail("client@example.com");
        clientUser.setRole(com.actvn.enotary.enums.Role.CLIENT);

        clientDetails = new CustomUserDetails(clientUser);
        clientAuth = new UsernamePasswordAuthenticationToken(clientDetails, null, clientDetails.getAuthorities());
    }

    @Test
    void createRequest_returnsCreated() throws Exception {
        NotaryRequestCreateRequest req = new NotaryRequestCreateRequest();
        req.setServiceType(ServiceType.ONLINE);
        req.setContractType(ContractType.TRANSFER_OF_PROPERTY);
        req.setDescription("Test create");

        NotaryRequest created = new NotaryRequest();
        created.setRequestId(UUID.randomUUID());
        created.setClient(clientUser);
        created.setServiceType(ServiceType.ONLINE);
        created.setContractType(ContractType.TRANSFER_OF_PROPERTY);
        created.setDescription("Test create");
        created.setStatus(RequestStatus.NEW);
        created.setCreatedAt(OffsetDateTime.now());
        created.setUpdatedAt(OffsetDateTime.now());

        when(notaryRequestService.createRequest(eq(clientUser.getEmail()), any(NotaryRequestCreateRequest.class)))
                .thenReturn(created);

        mockMvc.perform(post("/api/requests")
                        .principal(clientAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.requestId").value(created.getRequestId().toString()))
                .andExpect(jsonPath("$.clientId").value(clientUser.getUserId().toString()))
                .andExpect(jsonPath("$.serviceType").value("ONLINE"))
                .andExpect(jsonPath("$.contractType").value("TRANSFER_OF_PROPERTY"));
    }

    @Test
    void getRequest_ownerCanView() throws Exception {
        UUID rid = UUID.randomUUID();
        NotaryRequest r = new NotaryRequest();
        r.setRequestId(rid);
        r.setClient(clientUser);
        r.setServiceType(ServiceType.OFFLINE);
        r.setContractType(ContractType.WILL);
        r.setDescription("desc");
        r.setStatus(RequestStatus.NEW);
        r.setCreatedAt(OffsetDateTime.now());
        r.setUpdatedAt(OffsetDateTime.now());

        when(notaryRequestService.getById(rid)).thenReturn(r);

        mockMvc.perform(get("/api/requests/" + rid)
                        .principal(clientAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(rid.toString()))
                .andExpect(jsonPath("$.clientId").value(clientUser.getUserId().toString()));
    }

    @Test
    void getRequest_forbiddenForOtherUser() throws Exception {
        UUID rid = UUID.randomUUID();
        NotaryRequest r = new NotaryRequest();
        r.setRequestId(rid);
        // owner is different
        User other = new User();
        other.setUserId(UUID.randomUUID());
        other.setEmail("other@example.com");
        r.setClient(other);

        when(notaryRequestService.getById(rid)).thenReturn(r);

        mockMvc.perform(get("/api/requests/" + rid)
                        .principal(clientAuth))
                .andExpect(status().isForbidden());
    }

    @Test
    void listMyRequests_returnsList() throws Exception {
        UUID rid = UUID.randomUUID();
        NotaryRequest r = new NotaryRequest();
        r.setRequestId(rid);
        r.setClient(clientUser);
        r.setServiceType(ServiceType.OFFLINE);
        r.setContractType(ContractType.WILL);
        r.setDescription("desc");
        r.setStatus(RequestStatus.NEW);
        r.setCreatedAt(OffsetDateTime.now());
        r.setUpdatedAt(OffsetDateTime.now());

        when(notaryRequestService.listForClient(clientUser.getUserId())).thenReturn(List.of(r));

        mockMvc.perform(get("/api/requests/me").principal(clientAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(rid.toString()))
                .andExpect(jsonPath("$[0].clientId").value(clientUser.getUserId().toString()));
    }

    @Test
    void uploadDocument_allowsOwner() throws Exception {
        UUID rid = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

        Document doc = new Document();
        doc.setDocumentId(UUID.randomUUID());

        when(notaryRequestService.uploadDocument(eq(rid), eq(clientUser.getEmail()), any(), eq(DocType.DRAFT_CONTRACT)))
                .thenReturn(doc);

        mockMvc.perform(multipart("/api/requests/" + rid + "/documents")
                        .file(file)
                        .param("docType", "DRAFT_CONTRACT")
                        .principal(clientAuth))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
    }

    @Test
    void cancelRequest_ownerCanCancel() throws Exception {
        UUID rid = UUID.randomUUID();
        NotaryRequest r = new NotaryRequest();
        r.setRequestId(rid);
        r.setClient(clientUser);
        r.setStatus(RequestStatus.NEW);

        NotaryRequest updated = new NotaryRequest();
        updated.setRequestId(rid);
        updated.setClient(clientUser);
        updated.setStatus(RequestStatus.CANCELLED);

        when(notaryRequestService.cancelRequest(eq(rid), eq(clientUser.getEmail()))).thenReturn(updated);

        mockMvc.perform(post("/api/requests/" + rid + "/cancel").principal(clientAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelRequest_forbiddenForOtherUser() throws Exception {
        UUID rid = UUID.randomUUID();
        // service will throw AppException with FORBIDDEN
        when(notaryRequestService.cancelRequest(eq(rid), eq(clientUser.getEmail())))
                .thenThrow(new com.actvn.enotary.exception.AppException("Không có quyền hủy yêu cầu này", org.springframework.http.HttpStatus.FORBIDDEN));

        mockMvc.perform(post("/api/requests/" + rid + "/cancel").principal(clientAuth))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelRequest_cannotCancelCompleted() throws Exception {
        UUID rid = UUID.randomUUID();
        when(notaryRequestService.cancelRequest(eq(rid), eq(clientUser.getEmail())))
                .thenThrow(new com.actvn.enotary.exception.AppException("Không thể hủy yêu cầu đã hoàn thành", org.springframework.http.HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/api/requests/" + rid + "/cancel").principal(clientAuth))
                .andExpect(status().isBadRequest());
    }

}
