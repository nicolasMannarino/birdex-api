package com.birdex.service;

import com.birdex.domain.UserPhotoRequest;
import com.birdex.domain.UserResponse;
import com.birdex.domain.UsernameRequest;
import com.birdex.dto.SightingDto;
import com.birdex.entity.SightingEntity;
import com.birdex.entity.UserEntity;
import com.birdex.exception.UserAlreadyExistsException;
import com.birdex.exception.UserNotFoundException;
import com.birdex.mapper.SightingMapper;
import com.birdex.repository.SightingRepository;
import com.birdex.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final SightingRepository sightingRepository;
    private final UserRepository userRepository;

    private static final String MSG_USER_NOT_FOUND_BY_EMAIL = "No encontramos una cuenta registrada con ese correo.";
    private static final String MSG_USER_NOT_FOUND_BY_USERNAME = "No encontramos una cuenta registrada con ese usuario.";

    public List<SightingDto> getSightingByEmail(String email) {
        return SightingMapper.toDtoList(sightingRepository.findByUserEmail(email));
    }

    public List<SightingEntity> getSightingsEntityByEmail(String email) {
        return sightingRepository.findByUserEmail(email);
    }

    public void updatePhoto(String email, UserPhotoRequest request) {
        log.info("Searching user with email: {}", email);
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(MSG_USER_NOT_FOUND_BY_EMAIL));

        log.info("User founded. - {}", email);
        //some validations??

        user.updateProfilePhotoBase64(request.getPhoto());
        userRepository.save(user);
        log.info("Profile photo updated.");
    }

    public void updateUsername(String email, UsernameRequest request) {
        log.info("Searching user with email: {}", email);
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(MSG_USER_NOT_FOUND_BY_EMAIL));

        log.info("User founded. - {}", email);

        Optional<UserEntity> existingUser = userRepository.findByUsername(request.getNewUsername());

        if (existingUser.isPresent()) {
            throw new UserAlreadyExistsException("Username not available");
        }

        user.updateUsername(request.getNewUsername());
        userRepository.save(user);
        log.info("Username updated.");
    }

    public UserResponse getUserInfo(String username){
        log.info("Searching user with username: {}", username);
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(MSG_USER_NOT_FOUND_BY_EMAIL));

        log.info("User founded. - {}", username);
        return UserResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .points(user.getPoints())
                .level(user.getLevel())
                .levelName(user.getLevelName())
                .profilePhotoBase64(user.getProfilePhotoBase64())
                .build();
    }
}
