package com.teensconf.controller;

import com.teensconf.dto.RegistrationRequest;
import com.teensconf.entity.Registration;
import com.teensconf.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping
    public ResponseEntity<?> createRegistration(@Valid @RequestBody RegistrationRequest request) {
        try {
            Registration registration = registrationService.createRegistration(request);

            Map<String, Object> response = new HashMap<>();
            response.put("registrationId", registration.getId());
            response.put("message", "Регистрация успешно создана. Пожалуйста, произведите оплату.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Ошибка при регистрации: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Service is healthy");
    }
}
