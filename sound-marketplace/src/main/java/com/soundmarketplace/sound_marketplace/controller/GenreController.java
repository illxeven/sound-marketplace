package com.soundmarketplace.sound_marketplace.controller;

import com.soundmarketplace.sound_marketplace.domain.Genre;
import com.soundmarketplace.sound_marketplace.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import com.soundmarketplace.sound_marketplace.dto.ApiResponse;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@RequestMapping("/api/genres")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GenreController {

    private final GenreRepository genreRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<String>>> getAllGenres() {
        List<String> genres = genreRepository.findAll().stream()
                .map(Genre::getName)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(genres));
    }
}
