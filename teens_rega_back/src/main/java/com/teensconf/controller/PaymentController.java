package com.teensconf.controller;

import com.teensconf.dto.PaymentCompletionRequest;
import com.teensconf.entity.PaymentReceipt;
import com.teensconf.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping(value = "/{registrationId}/complete", consumes = "multipart/form-data")
    public ResponseEntity<?> completeRegistration(
            @PathVariable Long registrationId,
            @ModelAttribute PaymentCompletionRequest request) {

        try {
            PaymentReceipt receipt = paymentService.processPaymentCompletion(registrationId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("receiptId", receipt.getId());
            response.put("verified", receipt.getVerified());
            response.put("message", receipt.getVerified() ?
                    "Регистрация успешно завершена! Проверьте вашу почту." :
                    "Данные об оплате получены. Ожидайте проверки.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Ошибка при завершении регистрации: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}