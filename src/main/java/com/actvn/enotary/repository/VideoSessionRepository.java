package com.actvn.enotary.repository;

import com.actvn.enotary.entity.VideoSession;
import com.actvn.enotary.enums.VideoSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoSessionRepository extends JpaRepository<VideoSession, UUID> {

    /**
     * Tìm video session bằng appointment ID
     */
    Optional<VideoSession> findByAppointmentAppointmentId(UUID appointmentId);

    /**
     * Kiểm tra xem appointment đã có video session chưa
     */
    boolean existsByAppointmentAppointmentId(UUID appointmentId);

    /**
     * Tìm video session bằng room ID
     */
    Optional<VideoSession> findByRoomId(String roomId);

    /**
     * Tìm video session bằng session token
     */
    Optional<VideoSession> findBySessionToken(String sessionToken);
}

