package com.birdex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role; // "USER" | "ADMIN"

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "points", nullable = false)
    @Builder.Default
    private Integer points = 0;

    @Column(name = "level", nullable = false)
    @Builder.Default
    private Integer level = 1;

    @Column(name = "level_name", nullable = false)
    @Builder.Default
    private String levelName = "Novato";

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @ToString.Exclude
    @Column(name = "profile_photo_base64", columnDefinition = "TEXT")
    private String profilePhotoBase64;


    public void updateProfilePhotoBase64(String photo) {
        this.profilePhotoBase64 = photo;
    }

}