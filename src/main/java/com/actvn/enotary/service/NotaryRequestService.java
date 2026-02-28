package com.actvn.enotary.service;

import com.actvn.enotary.dto.request.NotaryRequestCreateRequest;
import com.actvn.enotary.entity.Document;
import com.actvn.enotary.entity.NotaryRequest;
import com.actvn.enotary.entity.User;
import com.actvn.enotary.enums.DocType;
import com.actvn.enotary.enums.RequestStatus;
import com.actvn.enotary.exception.AppException;
import com.actvn.enotary.repository.DocumentRepository;
import com.actvn.enotary.repository.NotaryRequestRepository;
import com.actvn.enotary.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotaryRequestService {
    private final NotaryRequestRepository notaryRequestRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    @Transactional
    public NotaryRequest createRequest(String clientEmail, NotaryRequestCreateRequest req) {
        // find client by email
        User client = userRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new AppException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        NotaryRequest r = new NotaryRequest();
        r.setClient(client);
        r.setNotary(null); // not yet assigned
        r.setServiceType(req.getServiceType());
        r.setContractType(req.getContractType());
        r.setDescription(req.getDescription());
        r.setStatus(com.actvn.enotary.enums.RequestStatus.NEW);
        r.setCreatedAt(OffsetDateTime.now());
        r.setUpdatedAt(OffsetDateTime.now());

        return notaryRequestRepository.save(r);
    }

    public NotaryRequest getById(UUID requestId) {
        return notaryRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException("Không tìm thấy yêu cầu công chứng", HttpStatus.NOT_FOUND));
    }

    public List<NotaryRequest> listForClient(UUID userId) {
        return notaryRequestRepository.findByClientUserId(userId);
    }

    // New: paginated listing for notary with filter by status
    public Page<NotaryRequest> listForNotaryByStatus(UUID notaryUserId, RequestStatus status, Pageable pageable) {
        if (status == RequestStatus.NEW) {
            return notaryRequestRepository.findByStatus(status, pageable);
        }
        return notaryRequestRepository.findByNotaryUserIdAndStatus(notaryUserId, status, pageable);
    }

    // Overload to accept PageRequest specifically (resolves compilation edge cases where compiler expects exact PageRequest type)
    public Page<NotaryRequest> listForNotaryByStatus(UUID notaryUserId, RequestStatus status, org.springframework.data.domain.PageRequest pageRequest) {
        return listForNotaryByStatus(notaryUserId, status, (Pageable) pageRequest);
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
        // fallback to cwd
        return cwd;
    }

    public Path getProjectRootPublic() {
        return findProjectRoot();
    }

    @Transactional
    public Document uploadDocument(UUID requestId, String uploaderEmail, MultipartFile file, DocType docType) {
        NotaryRequest request = getById(requestId);

        // authorize: only client owner, assigned notary or admin can upload
        User uploader = userRepository.findByEmail(uploaderEmail)
                .orElseThrow(() -> new AppException("Kh��ng tìm thấy người dùng", HttpStatus.NOT_FOUND));

        boolean isOwner = request.getClient() != null && request.getClient().getUserId().equals(uploader.getUserId());
        boolean isAssignedNotary = request.getNotary() != null && request.getNotary().getUserId().equals(uploader.getUserId());
        boolean isAdmin = uploader.getRole() != null && uploader.getRole().name().equals("ADMIN");

        if (!isOwner && !isAssignedNotary && !isAdmin) {
            throw new AppException("Không có quyền upload hồ sơ cho yêu cầu này", HttpStatus.FORBIDDEN);
        }

        try {
            // determine project root (search for pom.xml) and use projectRoot/uploads
            Path projectRoot = findProjectRoot();
            Path uploadsDir = projectRoot.resolve("uploads");

            Files.createDirectories(uploadsDir);

            String filename = UUID.randomUUID() + "-" + file.getOriginalFilename();
            Path target = uploadsDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            byte[] bytes = Files.readAllBytes(target);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            String hash = HexFormat.of().formatHex(digest);

            Document doc = new Document();
            doc.setRequest(request);
            // store relative path from project root, e.g. uploads/uuid-filename
            Path relative = projectRoot.relativize(target);
            doc.setFilePath(relative.toString().replace("\\", "/"));
            doc.setDocType(docType);
            doc.setFileHash(hash);
            doc.setCreatedAt(OffsetDateTime.now());

            return documentRepository.save(doc);
        } catch (IOException ex) {
            throw new AppException("Lỗi khi lưu file", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception ex) {
            throw new AppException("Lỗi khi tính toán hash file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public NotaryRequest cancelRequest(UUID requestId, String requesterEmail) {
        NotaryRequest request = getById(requestId);

        // load requester
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new AppException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        boolean isOwner = request.getClient() != null && request.getClient().getUserId().equals(requester.getUserId());
        boolean isAdmin = requester.getRole() != null && requester.getRole().name().equals("ADMIN");

        if (!isOwner && !isAdmin) {
            throw new AppException("Không có quyền hủy yêu cầu này", HttpStatus.FORBIDDEN);
        }

        if (request.getStatus() == com.actvn.enotary.enums.RequestStatus.COMPLETED) {
            throw new AppException("Không thể hủy yêu cầu đã hoàn thành", HttpStatus.BAD_REQUEST);
        }

        request.setStatus(com.actvn.enotary.enums.RequestStatus.CANCELLED);
        request.setUpdatedAt(OffsetDateTime.now());
        return notaryRequestRepository.save(request);
    }
}
