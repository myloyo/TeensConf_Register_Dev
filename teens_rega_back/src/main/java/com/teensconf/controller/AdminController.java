package com.teensconf.controller;

import com.teensconf.entity.Registration;
import com.teensconf.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final RegistrationRepository registrationRepository;

    @GetMapping("/registrations")
    public ResponseEntity<Page<Registration>> getRegistrations(Pageable pageable) {
        return ResponseEntity.ok(registrationRepository.findAll(pageable));
    }

    @GetMapping("/registrations/{id}")
    public ResponseEntity<Registration> getRegistration(@PathVariable Long id) {
        return registrationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRegistrations", registrationRepository.count());
        stats.put("completedRegistrations", registrationRepository.countByRegistrationCompletedAtIsNotNull());
        stats.put("pendingRegistrations", registrationRepository.countByRegistrationCompletedAtIsNull());
        return ResponseEntity.ok(stats);
    }
}