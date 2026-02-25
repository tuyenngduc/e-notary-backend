package com.actvn.enotary.service;

import com.actvn.enotary.dto.request.NotaryRequestCreateRequest;
import com.actvn.enotary.entity.NotaryRequest;
import com.actvn.enotary.entity.User;
import com.actvn.enotary.enums.RequestStatus;
import com.actvn.enotary.exception.AppException;
import com.actvn.enotary.repository.DocumentRepository;
import com.actvn.enotary.repository.NotaryRequestRepository;
import com.actvn.enotary.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotaryRequestServiceTest {

    @Mock
    NotaryRequestRepository notaryRequestRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    DocumentRepository documentRepository;

    NotaryRequestService service;

    @BeforeEach
    void setUp() {
        service = new NotaryRequestService(notaryRequestRepository, userRepository, documentRepository);
    }

    @Test
    void cancelRequest_ownerCancels_success() {
        UUID rid = UUID.randomUUID();
        UUID uid = UUID.randomUUID();

        User client = new User();
        client.setUserId(uid);
        client.setEmail("a@b.com");

        NotaryRequest r = new NotaryRequest();
        r.setRequestId(rid);
        r.setClient(client);
        r.setStatus(RequestStatus.NEW);

        when(notaryRequestRepository.findById(rid)).thenReturn(Optional.of(r));
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(client));
        when(notaryRequestRepository.save(any(NotaryRequest.class))).thenAnswer(i -> i.getArgument(0));

        NotaryRequest out = service.cancelRequest(rid, "a@b.com");
        assertEquals(RequestStatus.CANCELLED, out.getStatus());
    }

    @Test
    void cancelRequest_forbiddenForNonOwner() {
        UUID rid = UUID.randomUUID();
        User client = new User();
        client.setUserId(UUID.randomUUID());
        client.setEmail("owner@example.com");

        NotaryRequest r = new NotaryRequest();
        r.setRequestId(rid);
        r.setClient(client);
        r.setStatus(RequestStatus.NEW);

        User other = new User();
        other.setUserId(UUID.randomUUID());
        other.setEmail("other@example.com");

        when(notaryRequestRepository.findById(rid)).thenReturn(Optional.of(r));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(other));

        AppException ex = assertThrows(AppException.class, () -> service.cancelRequest(rid, "other@example.com"));
        assertEquals(403, ex.getStatus().value());
    }

    @Test
    void cancelRequest_cannotCancelCompleted() {
        UUID rid = UUID.randomUUID();
        User client = new User();
        client.setUserId(UUID.randomUUID());
        client.setEmail("a@b.com");

        NotaryRequest r = new NotaryRequest();
        r.setRequestId(rid);
        r.setClient(client);
        r.setStatus(RequestStatus.COMPLETED);

        when(notaryRequestRepository.findById(rid)).thenReturn(Optional.of(r));
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(client));

        AppException ex = assertThrows(AppException.class, () -> service.cancelRequest(rid, "a@b.com"));
        assertEquals(400, ex.getStatus().value());
    }
}

