package com.teensconf.controller;

import com.teensconf.dto.PaymentCompletionRequest;
import com.teensconf.entity.PaymentReceipt;
import com.teensconf.entity.Registration;
import com.teensconf.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping(value = "/{registrationId}/complete", consumes = "multipart/form-data")
    public ResponseEntity<?> completeRegistration(
            @PathVariable Long registrationId,
            @RequestParam(value = "receiptFile", required = false) MultipartFile receiptFile) {  // ← Изменил параметр

        log.info("Received payment completion request for registrationId: {}", registrationId);
        log.info("File received: {}", receiptFile != null ? receiptFile.getOriginalFilename() : "null");

        try {
            PaymentCompletionRequest request = new PaymentCompletionRequest();
            request.setReceiptFile(receiptFile);

            PaymentReceipt receipt = paymentService.processPaymentCompletion(registrationId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("receiptId", receipt.getId());
            response.put("verified", receipt.getVerified());
            response.put("message", receipt.getVerified() ?
                    "Регистрация успешно завершена! Проверьте вашу почту." :
                    "Данные об оплате получены. Ожидайте проверки.");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Ошибка при завершении регистрации: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

}