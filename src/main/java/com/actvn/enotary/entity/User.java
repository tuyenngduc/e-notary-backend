package com.actvn.enotary.entity;

import com.actvn.enotary.enums.Role;
import com.actvn.enotary.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;
    private String email;
    private String phoneNumber;
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private UserProfile profile;
}