package com.birdex.service;

import com.birdex.dto.SightingDto;
import com.birdex.entity.SightingEntity;
import com.birdex.mapper.SightingMapper;
import com.birdex.repository.SightingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SightingRepository sightingRepository;

    public List<SightingDto> getSightingByEmail(String email){
        return SightingMapper.toDtoList(sightingRepository.findByUserEmail(email));
    }

    public List<SightingEntity> getSightingsEntityByEmail(String email){
        return sightingRepository.findByUserEmail(email);
    }
}
