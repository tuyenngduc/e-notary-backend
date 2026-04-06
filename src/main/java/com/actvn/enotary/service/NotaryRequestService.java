package com.actvn.enotary.service;

import com.actvn.enotary.dto.request.NotaryRequestCreateRequest;
import com.actvn.enotary.dto.request.ScheduleAppointmentRequest;
import com.actvn.enotary.dto.response.AppointmentResponse;
import com.actvn.enotary.dto.response.DocumentRequirementResponse;
import com.actvn.enotary.entity.Appointment;
import com.actvn.enotary.entity.Document;
import com.actvn.enotary.entity.NotaryRequest;
import com.actvn.enotary.entity.User;
import com.actvn.enotary.entity.VideoSession;
import com.actvn.enotary.enums.AppointmentStatus;
import com.actvn.enotary.enums.DocType;
import com.actvn.enotary.enums.RequestStatus;
import com.actvn.enotary.enums.ServiceType;
import com.actvn.enotary.enums.VideoSessionStatus;
import com.actvn.enotary.exception.AppException;
import com.actvn.enotary.exception.ErrorCode;
import com.actvn.enotary.repository.AppointmentRepository;
import com.actvn.enotary.repository.DocumentRepository;
import com.actvn.enotary.repository.NotaryRequestRepository;
import com.actvn.enotary.repository.UserRepository;
import com.actvn.enotary.repository.VideoSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotaryRequestService {
    private final NotaryRequestRepository notaryRequestRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final AppointmentRepository appointmentRepository;
    private final VideoSessionRepository videoSessionRepository;
    private final AppointmentEmailService appointmentEmailService;

    @Value("${app.meeting.base-url:http://localhost:8080}")
    private String baseUrl;

    private boolean isClaimedByAnotherNotary(NotaryRequest request, User reviewer) {
        boolean isNotary = reviewer.getRole() != null && reviewer.getRole().name().equals("NOTARY");
        return isNotary
                && request.getNotary() != null
                && !request.getNotary().getUserId().equals(reviewer.getUserId());
    }

    @Transactional
    public NotaryRequest createRequest(String clientEmail, NotaryRequestCreateRequest req) {
        User client = userRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new AppException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        NotaryRequest r = new NotaryRequest();
        r.setClient(client);
        r.setNotary(null);
        r.setServiceType(req.getServiceType());
        r.setContractType(req.getContractType());
        r.setDescription(req.getDescription());
        r.setStatus(RequestStatus.NEW);
        r.setCreatedAt(OffsetDateTime.now());
        r.setUpdatedAt(OffsetDateTime.now());

        return notaryRequestRepository.save(r);
    }

    public NotaryRequest getById(UUID requestId) {
        return notaryRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException("Không tìm thấy yêu cầu công chứng", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public NotaryRequest acceptRequest(UUID requestId, String notaryEmail) {
        User notary = userRepository.findByEmail(notaryEmail)
                .orElseThrow(() -> new AppException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        boolean isNotary = notary.getRole() != null && notary.getRole().name().equals("NOTARY");
        if (!isNotary) {
            throw new AppException("Chỉ công chứng viên mới có quyền tiếp nhận yêu cầu", HttpStatus.FORBIDDEN);
        }

        NotaryRequest request = notaryRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new AppException("Không tìm thấy yêu cầu công chứng", HttpStatus.NOT_FOUND));

        if (isClaimedByAnotherNotary(request, notary)) {
            throw alreadyAssignedException();
        }

        // Idempotent: same notary can call accept again after request already moved to PROCESSING.
        if (request.getStatus() == RequestStatus.PROCESSING
                && request.getNotary() != null
                && request.getNotary().getUserId().equals(notary.getUserId())) {
            return request;
        }

        if (request.getStatus() != RequestStatus.NEW) {
            throw new AppException("Yêu cầu không ở trạng thái NEW để tiếp nhận", HttpStatus.BAD_REQUEST);
        }

        DocumentRequirementResponse documentRequirements = buildDocumentRequirements(request);
        if (!documentRequirements.isReadyForAccept()) {
            String missing = documentRequirements.getMissingDocTypes().stream().map(Enum::name).collect(java.util.stream.Collectors.joining(", "));
            throw new AppException(
                    ErrorCode.REQUEST_MISSING_REQUIRED_DOCUMENTS,
                    Map.of("missingDocTypes", documentRequirements.getMissingDocTypes().stream().map(Enum::name).toList())
            );
        }

        request.setNotary(notary);
        request.setStatus(RequestStatus.PROCESSING);
        request.setUpdatedAt(OffsetDateTime.now());
        return notaryRequestRepository.save(request);
    }

    private Set<DocType> requiredDocTypesForAccept(NotaryRequest request) {
        EnumSet<DocType> required = EnumSet.of(DocType.ID_CARD, DocType.DRAFT_CONTRACT);
        if (request.getContractType() == com.actvn.enotary.enums.ContractType.TRANSFER_OF_PROPERTY) {
            required.add(DocType.PROPERTY_PAPER);
        }
        return required;
    }

    public DocumentRequirementResponse getDocumentRequirements(UUID requestId) {
        return buildDocumentRequirements(getById(requestId));
    }

    private DocumentRequirementResponse buildDocumentRequirements(NotaryRequest request) {
        List<DocType> uploadedDocTypes = sortDocTypes(documentRepository.findDocTypesByRequestId(request.getRequestId()));
        Set<DocType> uploadedSet = uploadedDocTypes.isEmpty()
                ? EnumSet.noneOf(DocType.class)
                : EnumSet.copyOf(uploadedDocTypes);

        List<DocType> requiredDocTypes = sortDocTypes(requiredDocTypesForAccept(request));
        List<DocType> missingDocTypes = requiredDocTypes.stream()
                .filter(requiredType -> !uploadedSet.contains(requiredType))
                .toList();

        return DocumentRequirementResponse.builder()
                .requiredDocTypes(requiredDocTypes)
                .uploadedDocTypes(uploadedDocTypes)
                .missingDocTypes(missingDocTypes)
                .readyForAccept(missingDocTypes.isEmpty())
                .build();
    }

    private List<DocType> sortDocTypes(Iterable<DocType> docTypes) {
        EnumSet<DocType> unique = EnumSet.noneOf(DocType.class);
        for (DocType docType : docTypes) {
            if (docType != null) {
                unique.add(docType);
            }
        }
        return unique.stream().toList();
    }

    public List<NotaryRequest> listForClient(UUID userId) {
        return notaryRequestRepository.findByClientUserId(userId);
    }

    // When status is null return NEW + requests assigned to this notary.
    public Page<NotaryRequest> listForNotaryByStatus(UUID notaryUserId, RequestStatus status, Pageable pageable) {
        if (status == null) {
            return notaryRequestRepository.findByStatusOrNotaryUserId(RequestStatus.NEW, notaryUserId, pageable);
        }
        if (status == RequestStatus.NEW) {
            return notaryRequestRepository.findByStatus(status, pageable);
        }
        return notaryRequestRepository.findByNotaryUserIdAndStatus(notaryUserId, status, pageable);
    }

    public Page<NotaryRequest> listForNotaryByStatus(UUID notaryUserId, RequestStatus status, org.springframework.data.domain.PageRequest pageRequest) {
        return listForNotaryByStatus(notaryUserId, status, (Pageable) pageRequest);
    }

    public Page<NotaryRequest> listAcceptedByNotary(UUID notaryUserId, Pageable pageable) {
        return notaryRequestRepository.findByNotaryUserId(notaryUserId, pageable);
    }

    private Path findProjectRoot() {
        Path cwd = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path cur = cwd;
        while (cur != null) {
            if (Files.exists(cur.resolve("pom.xml"))) {
                return cur;
            }
            cur = cur.getParent();
        }
        return cwd;
    }

    public Path getProjectRootPublic() {
        return findProjectRoot();
    }

    public List<Document> getDocumentsByRequestId(UUID requestId) {
        return documentRepository.findByRequest_RequestId(requestId);
    }

    @Transactional
    public Document uploadDocument(UUID requestId, String uploaderEmail, MultipartFile file, DocType docType) {
        NotaryRequest request = getById(requestId);

        User uploader = userRepository.findByEmail(uploaderEmail)
                .orElseThrow(() -> new AppException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        boolean isOwner = request.getClient() != null && request.getClient().getUserId().equals(uploader.getUserId());
        boolean isAssignedNotary = request.getNotary() != null && request.getNotary().getUserId().equals(uploader.getUserId());
        boolean isAdmin = uploader.getRole() != null && uploader.getRole().name().equals("ADMIN");

        if (!isOwner && !isAssignedNotary && !isAdmin) {
            throw new AppException("Không có quyền upload hồ sơ cho yêu cầu này", HttpStatus.FORBIDDEN);
        }

        validateRequestIsNotTerminal(request);

        StoredFileResult stored = storeFile(file);

        Document doc = new Document();
        doc.setRequest(request);
        doc.setFilePath(stored.relativePath());
        doc.setDocType(docType);
        doc.setFileHash(stored.hash());
        doc.setCreatedAt(OffsetDateTime.now());

        return documentRepository.save(doc);
    }

    @Transactional
    public Document replaceDocument(UUID documentId, String uploaderEmail, MultipartFile file) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        NotaryRequest request = doc.getRequest();
        User uploader = userRepository.findByEmail(uploaderEmail)
                .orElseThrow(() -> new AppException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        boolean isOwner = request.getClient() != null && request.getClient().getUserId().equals(uploader.getUserId());
        boolean isAssignedNotary = request.getNotary() != null && request.getNotary().getUserId().equals(uploader.getUserId());
        boolean isAdmin = uploader.getRole() != null && uploader.getRole().name().equals("ADMIN");

        if (!isOwner && !isAssignedNotary && !isAdmin) {
            throw new AppException("Không có quyền cập nhật tài liệu cho yêu cầu này", HttpStatus.FORBIDDEN);
        }

        validateRequestIsNotTerminal(request);
        validateDocumentCanBeReplaced(doc);

        StoredFileResult stored = storeFile(file);
        doc.setFilePath(stored.relativePath());
        doc.setFileHash(stored.hash());
        doc.setUpdatedAt(OffsetDateTime.now());
        return documentRepository.save(doc);
    }

    private StoredFileResult storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException("File tải lên không hợp lệ", HttpStatus.BAD_REQUEST);
        }

        try {
            Path projectRoot = findProjectRoot();
            Path uploadsDir = projectRoot.resolve("uploads").normalize();
            Files.createDirectories(uploadsDir);

            String originalName = file.getOriginalFilename() == null ? "document.bin" : file.getOriginalFilename();
            String safeName = Path.of(originalName).getFileName().toString();
            if (safeName.isBlank()) {
                safeName = "document.bin";
            }

            String filename = UUID.randomUUID() + "-" + safeName;
            Path target = uploadsDir.resolve(filename).normalize();
            if (!target.startsWith(uploadsDir)) {
                throw new AppException("Tên file không hợp lệ", HttpStatus.BAD_REQUEST);
            }

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            byte[] bytes = Files.readAllBytes(target);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            String hash = HexFormat.of().formatHex(digest);

            Path relative = projectRoot.relativize(target);
            return new StoredFileResult(relative.toString().replace("\\", "/"), hash);
        } catch (IOException ex) {
            throw new AppException("Lỗi khi lưu file", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception ex) {
            throw new AppException("Lỗi khi tính toán hash file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateRequestIsNotTerminal(NotaryRequest request) {
        if (request.getStatus() == RequestStatus.REJECTED
                || request.getStatus() == RequestStatus.CANCELLED
                || request.getStatus() == RequestStatus.COMPLETED) {
            throw new AppException(
                    ErrorCode.REQUEST_TERMINAL_STATUS,
                    Map.of("status", request.getStatus().name())
            );
        }
    }

    private void validateDocumentCanBeReplaced(Document doc) {
        DocType type = doc.getDocType();
        EnumSet<DocType> replaceableTypes = EnumSet.of(
                DocType.ID_CARD,
                DocType.PROPERTY_PAPER,
                DocType.DRAFT_CONTRACT,
                DocType.SESSION_VIDEO
        );

        if (!replaceableTypes.contains(type)) {
            throw new AppException(
                    ErrorCode.DOCUMENT_REPLACE_NOT_ALLOWED,
                    Map.of("docType", type.name())
            );
        }

        if (type == DocType.SESSION_VIDEO) {
            List<DocType> requestDocTypes = documentRepository.findDocTypesByRequestId(doc.getRequest().getRequestId());
            if (requestDocTypes.contains(DocType.SIGNED_DOCUMENT)) {
                throw new AppException(
                        ErrorCode.DOCUMENT_REPLACE_NOT_ALLOWED,
                        Map.of("docType", type.name(), "reason", "SIGNED_DOCUMENT_EXISTS")
                );
            }
        }
    }

    private record StoredFileResult(String relativePath, String hash) {
    }

    @Transactional
    public NotaryRequest cancelRequest(UUID requestId, String requesterEmail) {
        NotaryRequest request = getById(requestId);

        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new AppException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        boolean isOwner = request.getClient() != null && request.getClient().getUserId().equals(requester.getUserId());
        boolean isAdmin = requester.getRole() != null && requester.getRole().name().equals("ADMIN");

        if (!isOwner && !isAdmin) {
            throw new AppException("Không có quyền hủy yêu cầu này", HttpStatus.FORBIDDEN);
        }

        if (request.getStatus() == RequestStatus.COMPLETED) {
            throw new AppException("Không thể hủy yêu cầu đã hoàn thành", HttpStatus.BAD_REQUEST);
        }

        request.setStatus(RequestStatus.CANCELLED);
        request.setUpdatedAt(OffsetDateTime.now());
        return notaryRequestRepository.save(request);
    }

    @Transactional
    public AppointmentResponse scheduleAppointment(UUID requestId, String notaryEmail, ScheduleAppointmentRequest req) {
        NotaryRequest request = getById(requestId);

        User reviewer = userRepository.findByEmail(notaryEmail)
                .orElseThrow(() -> new AppException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        boolean isAdmin = reviewer.getRole() != null && reviewer.getRole().name().equals("ADMIN");
        boolean isAssignedNotary = reviewer.getRole() != null
                && reviewer.getRole().name().equals("NOTARY")
                && request.getNotary() != null
                && request.getNotary().getUserId().equals(reviewer.getUserId());

        if (isClaimedByAnotherNotary(request, reviewer)) {
            throw alreadyAssignedException();
        }

        if (!isAdmin && !isAssignedNotary) {
            throw new AppException("Không có quyền lên lịch cho yêu cầu này", HttpStatus.FORBIDDEN);
        }

        if (request.getStatus() != RequestStatus.PROCESSING) {
            throw new AppException(
                    "Chỉ có thể lên lịch khi yêu cầu đang ở trạng thái PROCESSING (hiện tại: " + request.getStatus() + ")",
                    HttpStatus.BAD_REQUEST);
        }

        if (appointmentRepository.existsByRequestRequestId(requestId)) {
            throw new AppException("Yêu cầu này đã có lịch hẹn", HttpStatus.CONFLICT);
        }

        Appointment appointment = new Appointment();
        appointment.setRequest(request);
        appointment.setScheduledTime(req.getScheduledTime());
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setCreatedAt(OffsetDateTime.now());

        if (request.getServiceType() == ServiceType.OFFLINE) {
            String address = (req.getPhysicalAddress() != null && !req.getPhysicalAddress().isBlank())
                    ? req.getPhysicalAddress()
                    : "Văn phòng công chứng số 1";
            appointment.setPhysicalAddress(address);
            appointment.setMeetingUrl(null);
        } else {
            appointment.setPhysicalAddress(null);
        }

        Appointment saved = appointmentRepository.save(appointment);

        if (request.getServiceType() == ServiceType.ONLINE) {
            VideoSession session = new VideoSession();
            session.setAppointment(saved);

            String roomId = "room_" + UUID.randomUUID().toString().substring(0, 8);
            String sessionToken = UUID.randomUUID().toString();

            session.setRoomId(roomId);
            session.setSessionToken(sessionToken);

            String meetingUrl = baseUrl + "/api/video/room/" + roomId + "?token=" + sessionToken;
            session.setMeetingUrl(meetingUrl);
            session.setStatus(VideoSessionStatus.PENDING);
            session.setCreatedAt(OffsetDateTime.now());
            session.setUpdatedAt(OffsetDateTime.now());

            videoSessionRepository.save(session);

            saved.setMeetingUrl(meetingUrl);
            appointmentRepository.save(saved);
        }

        request.setStatus(RequestStatus.SCHEDULED);
        request.setUpdatedAt(OffsetDateTime.now());
        notaryRequestRepository.save(request);

        if (request.getServiceType() == ServiceType.ONLINE) {
            appointmentEmailService.sendOnlineMeetingLinkToClient(request, saved);
        }

        return AppointmentResponse.fromEntity(saved);
    }

    @Transactional
    public NotaryRequest rejectRequest(UUID requestId, String reviewerEmail, String reason) {
        NotaryRequest request = getById(requestId);

        User reviewer = userRepository.findByEmail(reviewerEmail)
                .orElseThrow(() -> new AppException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        boolean isAdmin = reviewer.getRole() != null && reviewer.getRole().name().equals("ADMIN");
        boolean isAssignedNotary = reviewer.getRole() != null
                && reviewer.getRole().name().equals("NOTARY")
                && request.getNotary() != null
                && request.getNotary().getUserId().equals(reviewer.getUserId());

        if (isClaimedByAnotherNotary(request, reviewer)) {
            throw alreadyAssignedException();
        }

        if (!isAdmin && !isAssignedNotary) {
            throw new AppException("Không có quyền từ chối yêu cầu này", HttpStatus.FORBIDDEN);
        }

        if (request.getStatus() == RequestStatus.COMPLETED
                || request.getStatus() == RequestStatus.CANCELLED
                || request.getStatus() == RequestStatus.REJECTED) {
            throw new AppException("Không thể từ chối yêu cầu ở trạng thái hiện tại", HttpStatus.BAD_REQUEST);
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setRejectionReason(reason == null ? null : reason.trim());
        request.setUpdatedAt(OffsetDateTime.now());
        return notaryRequestRepository.save(request);
    }

    private AppException alreadyAssignedException() {
        return new AppException(ErrorCode.REQUEST_ALREADY_ASSIGNED);
    }
}
