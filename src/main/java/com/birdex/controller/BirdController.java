package com.birdex.controller;

import com.birdex.dto.BirdDto;
import com.birdex.dto.BirdProgressResponse;
import com.birdex.entity.BirdSummary;
import com.birdex.service.BirdService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/bird")
@CrossOrigin(origins = "*")
public class BirdController {

    private final BirdService birdService;

    public BirdController(BirdService birdService) {
        this.birdService = birdService;
    }

    @GetMapping("/all")
    public ResponseEntity<BirdProgressResponse> getBirds() {
        BirdProgressResponse response = birdService.getBirds();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/description/{commonName}")
    public ResponseEntity<String> getDescription(@PathVariable String commonName) {
        String response = birdService.getDescription(commonName);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{specificName}")
    public ResponseEntity<BirdDto> getBirdBySpecificName(@PathVariable String specificName) {
        BirdDto response = birdService.getBySpecificName(specificName);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public Page<BirdSummary> searchBirds(
            @RequestParam(required = false) String rarity,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String size,
            // @RequestParam(required = false) String zone,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int sizePage,
            @RequestParam(defaultValue = "name,asc") String sort
    ) {
        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(page, sizePage, sortObj);
        return birdService.search(rarity, color, size, pageable);
    }

    private Sort parseSort(String sort) {
        String[] parts = sort.split(",", 2);
        String field = parts[0].trim();
        Sort.Direction dir = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(dir, field);
    }
}
