package com.actvn.enotary.service;

import com.actvn.enotary.dto.request.ScheduleAppointmentRequest;
import com.actvn.enotary.dto.response.AppointmentResponse;
import com.actvn.enotary.entity.NotaryRequest;
import com.actvn.enotary.entity.User;
import com.actvn.enotary.enums.AppointmentStatus;
import com.actvn.enotary.enums.Role;
import com.actvn.enotary.enums.RequestStatus;
import com.actvn.enotary.enums.ServiceType;
import com.actvn.enotary.exception.AppException;
import com.actvn.enotary.repository.AppointmentRepository;
import com.actvn.enotary.repository.DocumentRepository;
import com.actvn.enotary.repository.NotaryRequestRepository;
import com.actvn.enotary.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
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

    @Mock
    AppointmentRepository appointmentRepository;

    NotaryRequestService service;

    @BeforeEach
    void setUp() {
        service = new NotaryRequestService(notaryRequestRepository, userRepository, documentRepository, appointmentRepository);
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

    @Test
    void rejectRequest_assignedNotaryRejects_success() {
        UUID rid = UUID.randomUUID();
        UUID nid = UUID.randomUUID();

        User notary = new User();
        notary.setUserId(nid);
        notary.setEmail("notary@example.com");
        notary.setRole(Role.NOTARY);

        NotaryRequest r = new NotaryRequest();
        r.setRequestId(rid);
        r.setNotary(notary);
        r.setStatus(RequestStatus.PROCESSING);

        when(notaryRequestRepository.findById(rid)).thenReturn(Optional.of(r));
        when(userRepository.findByEmail("notary@example.com")).thenReturn(Optional.of(notary));
        when(notaryRequestRepository.save(any(NotaryRequest.class))).thenAnswer(i -> i.getArgument(0));

        NotaryRequest out = service.rejectRequest(rid, "notary@example.com", "Thiếu hồ sơ gốc");
        assertEquals(RequestStatus.REJECTED, out.getStatus());
        assertEquals("Thiếu hồ sơ gốc", out.getRejectionReason());
    }

    @Test
    void rejectRequest_forbiddenForUnassignedNotary() {
        UUID rid = UUID.randomUUID();

        User assignedNotary = new User();
        assignedNotary.setUserId(UUID.randomUUID());

        NotaryRequest r = new NotaryRequest();
        r.setRequestId(rid);
        r.setNotary(assignedNotary);
        r.setStatus(RequestStatus.PROCESSING);

        User anotherNotary = new User();
        anotherNotary.setUserId(UUID.randomUUID());
        anotherNotary.setEmail("another-notary@example.com");
        anotherNotary.setRole(Role.NOTARY);

        when(notaryRequestRepository.findById(rid)).thenReturn(Optional.of(r));
        when(userRepository.findByEmail("another-notary@example.com")).thenReturn(Optional.of(anotherNotary));

        AppException ex = assertThrows(AppException.class,
                () -> service.rejectRequest(rid, "another-notary@example.com", "Không hợp lệ"));
        assertEquals(403, ex.getStatus().value());
    }

    @Test
    void scheduleAppointment_assignedNotarySchedules_offline_success() {
        UUID rid = UUID.randomUUID();
        UUID nid = UUID.randomUUID();

        User notary = new User();
        notary.setUserId(nid);
        notary.setEmail("notary@example.com");
        notary.setRole(Role.NOTARY);

        NotaryRequest r = new NotaryRequest();
        r.setRequestId(rid);
        r.setNotary(notary);
        r.setServiceType(ServiceType.OFFLINE);
        r.setStatus(RequestStatus.PROCESSING);

        ScheduleAppointmentRequest req = new ScheduleAppointmentRequest();
        req.setScheduledTime(OffsetDateTime.now().plusDays(3));
        req.setPhysicalAddress("VP CN Số 5");

        when(notaryRequestRepository.findById(rid)).thenReturn(Optional.of(r));
        when(userRepository.findByEmail("notary@example.com")).thenReturn(Optional.of(notary));
        when(appointmentRepository.existsByRequestRequestId(rid)).thenReturn(false);
        when(appointmentRepository.save(any())).thenAnswer(i -> {
            com.actvn.enotary.entity.Appointment a = i.getArgument(0);
            a.setAppointmentId(UUID.randomUUID());
            return a;
        });
        when(notaryRequestRepository.save(any(NotaryRequest.class))).thenAnswer(i -> i.getArgument(0));

        AppointmentResponse resp = service.scheduleAppointment(rid, "notary@example.com", req);

        assertEquals(AppointmentStatus.PENDING, resp.getStatus());
        assertEquals("VP CN Số 5", resp.getPhysicalAddress());
        assertNull(resp.getMeetingUrl());
        assertEquals(ServiceType.OFFLINE, resp.getServiceType());
        assertEquals(RequestStatus.SCHEDULED, r.getStatus());
    }

    @Test
    void scheduleAppointment_assignedNotarySchedules_online_noAddress() {
        UUID rid = UUID.randomUUID();
        UUID nid = UUID.randomUUID();

        User notary = new User();
        notary.setUserId(nid);
        notary.setEmail("notary@example.com");
        notary.setRole(Role.NOTARY);

        NotaryRequest r = new NotaryRequest();
        r.setRequestId(rid);
        r.setNotary(notary);
        r.setServiceType(ServiceType.ONLINE);
        r.setStatus(RequestStatus.PROCESSING);

        ScheduleAppointmentRequest req = new ScheduleAppointmentRequest();
        req.setScheduledTime(OffsetDateTime.now().plusDays(2));

        when(notaryRequestRepository.findById(rid)).thenReturn(Optional.of(r));
        when(userRepository.findByEmail("notary@example.com")).thenReturn(Optional.of(notary));
        when(appointmentRepository.existsByRequestRequestId(rid)).thenReturn(false);
        when(appointmentRepository.save(any())).thenAnswer(i -> {
            com.actvn.enotary.entity.Appointment a = i.getArgument(0);
            a.setAppointmentId(UUID.randomUUID());
            return a;
        });
        when(notaryRequestRepository.save(any(NotaryRequest.class))).thenAnswer(i -> i.getArgument(0));

        AppointmentResponse resp = service.scheduleAppointment(rid, "notary@example.com", req);

        assertNull(resp.getPhysicalAddress());
        assertNull(resp.getMeetingUrl());
        assertEquals(ServiceType.ONLINE, resp.getServiceType());
    }

    @Test
    void scheduleAppointment_wrongStatus_returns400() {
        UUID rid = UUID.randomUUID();
        UUID nid = UUID.randomUUID();

        User notary = new User();
        notary.setUserId(nid);
        notary.setEmail("notary@example.com");
        notary.setRole(Role.NOTARY);

        NotaryRequest r = new NotaryRequest();
        r.setRequestId(rid);
        r.setNotary(notary);
        r.setStatus(RequestStatus.NEW); // not PROCESSING

        ScheduleAppointmentRequest req = new ScheduleAppointmentRequest();
        req.setScheduledTime(OffsetDateTime.now().plusDays(1));

        when(notaryRequestRepository.findById(rid)).thenReturn(Optional.of(r));
        when(userRepository.findByEmail("notary@example.com")).thenReturn(Optional.of(notary));

        AppException ex = assertThrows(AppException.class,
                () -> service.scheduleAppointment(rid, "notary@example.com", req));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void scheduleAppointment_duplicateAppointment_returns409() {
        UUID rid = UUID.randomUUID();
        UUID nid = UUID.randomUUID();

        User notary = new User();
        notary.setUserId(nid);
        notary.setEmail("notary@example.com");
        notary.setRole(Role.NOTARY);

        NotaryRequest r = new NotaryRequest();
        r.setRequestId(rid);
        r.setNotary(notary);
        r.setStatus(RequestStatus.PROCESSING);

        ScheduleAppointmentRequest req = new ScheduleAppointmentRequest();
        req.setScheduledTime(OffsetDateTime.now().plusDays(1));

        when(notaryRequestRepository.findById(rid)).thenReturn(Optional.of(r));
        when(userRepository.findByEmail("notary@example.com")).thenReturn(Optional.of(notary));
        when(appointmentRepository.existsByRequestRequestId(rid)).thenReturn(true);

        AppException ex = assertThrows(AppException.class,
                () -> service.scheduleAppointment(rid, "notary@example.com", req));
        assertEquals(409, ex.getStatus().value());
    }

    @Test
    void scheduleAppointment_forbiddenForUnassignedNotary() {
        UUID rid = UUID.randomUUID();

        User assignedNotary = new User();
        assignedNotary.setUserId(UUID.randomUUID());

        NotaryRequest r = new NotaryRequest();
        r.setRequestId(rid);
        r.setNotary(assignedNotary);
        r.setStatus(RequestStatus.PROCESSING);

        User anotherNotary = new User();
        anotherNotary.setUserId(UUID.randomUUID());
        anotherNotary.setEmail("other@example.com");
        anotherNotary.setRole(Role.NOTARY);

        ScheduleAppointmentRequest req = new ScheduleAppointmentRequest();
        req.setScheduledTime(OffsetDateTime.now().plusDays(1));

        when(notaryRequestRepository.findById(rid)).thenReturn(Optional.of(r));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(anotherNotary));

        AppException ex = assertThrows(AppException.class,
                () -> service.scheduleAppointment(rid, "other@example.com", req));
        assertEquals(403, ex.getStatus().value());
    }
}

