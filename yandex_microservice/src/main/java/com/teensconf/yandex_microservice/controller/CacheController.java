package com.teensconf.yandex_microservice.controller;

import com.teensconf.yandex_microservice.dto.ExportRegistrationDTO;
import com.teensconf.yandex_microservice.service.RedisCacheService;
import com.teensconf.yandex_microservice.service.YandexExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {

    private final RedisCacheService redisCacheService;
    private final YandexExportService yandexExportService;

    @PostMapping("/clear")
    public ResponseEntity<String> clearCache() {
        redisCacheService.clearCache();
        return ResponseEntity.ok("Cache cleared");
    }

    @GetMapping("/all")
    public ResponseEntity<List<ExportRegistrationDTO>> getAllFromCache() {
        List<ExportRegistrationDTO> data = yandexExportService.getAllRegistrationsFromCache();
        return ResponseEntity.ok(data != null ? data : List.of());
    }

    @GetMapping("/paid")
    public ResponseEntity<List<ExportRegistrationDTO>> getPaidFromCache() {
        List<ExportRegistrationDTO> data = yandexExportService.getPaidRegistrationsFromCache();
        return ResponseEntity.ok(data != null ? data : List.of());
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshCache() {
        yandexExportService.forceCacheRefresh();
        return ResponseEntity.ok("Cache refresh initiated");
    }
}