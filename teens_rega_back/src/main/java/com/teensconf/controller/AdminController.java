package com.teensconf.controller;

import com.teensconf.entity.Registration;
import com.teensconf.entity.PaymentReceipt;
import com.teensconf.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final RegistrationRepository registrationRepository;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        log.info("Login attempt for username: {}", username);
        log.info("Expected username: {}, password: {}", adminUsername, adminPassword);

        if (adminUsername.equals(username) && adminPassword.equals(password)) {
            log.info("Login successful for user: {}", username);
            Map<String, String> response = new HashMap<>();
            response.put("token", "admin-auth-token");
            response.put("message", "Авторизация успешна");
            return ResponseEntity.ok(response);
        } else {
            log.info("Login failed for user: {}", username);
            return ResponseEntity.status(401).body(Map.of("message", "Неверные учетные данные"));
        }
    }

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

    @GetMapping("/registrations/{id}/receipt")
    public ResponseEntity<Resource> downloadReceipt(@PathVariable Long id) {
        try {
            Registration registration = registrationRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Регистрация не найдена"));

            PaymentReceipt receipt = registration.getPaymentReceipt();
            if (receipt == null) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = Paths.get(receipt.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + receipt.getFileName() + "\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error downloading receipt: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}