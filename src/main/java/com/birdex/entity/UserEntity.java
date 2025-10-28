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

    @Column(name = "profile_photo_key")
    private String profilePhotoKey;


    public void updateProfilePhotoKey(String key) { this.profilePhotoKey = key; }

    public void updateUsername(String newName) {
        this.username = newName;
    }

}