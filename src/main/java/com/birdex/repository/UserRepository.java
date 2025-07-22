package com.birdex.repository;

import com.birdex.entity.UserEntity;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {
    // Podés agregar métodos personalizados si querés, por ejemplo:
    Optional<UserEntity> findByEmail(String email);
}

