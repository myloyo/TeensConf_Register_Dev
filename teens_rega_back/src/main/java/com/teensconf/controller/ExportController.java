package com.teensconf.controller;

import com.teensconf.dto.ExportRegistrationDTO;
import com.teensconf.entity.PaymentReceipt;
import com.teensconf.entity.Registration;
import com.teensconf.repository.PaymentReceiptRepository;
import com.teensconf.repository.RegistrationRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/internal/export")
@RequiredArgsConstructor
public class ExportController {

    private final RegistrationRepository registrationRepository;
    private final PaymentReceiptRepository paymentReceiptRepository;

    @GetMapping("/paid-registrations")
    public ResponseEntity<List<ExportRegistrationDTO>> getPaidRegistrations() {
        List<Registration> all = registrationRepository.findAll();

        List<ExportRegistrationDTO> dtos = all.stream().map(r -> {
            ExportRegistrationDTO dto = new ExportRegistrationDTO();
            dto.setId(r.getId());
            dto.setFirstName(r.getFirstName());
            dto.setLastName(r.getLastName());
            dto.setEmail(r.getEmail());
            dto.setBirthDate(r.getBirthDate());
            dto.setPhone(r.getPhone());
            dto.setTelegram(r.getTelegram());
            dto.setCity(r.getCity());
            dto.setNeedAccommodation(r.getNeedAccommodation());
            dto.setChurch(r.getChurch());
            dto.setRole(r.getRole());
            dto.setParentFullName(r.getParentFullName());
            dto.setParentPhone(r.getParentPhone());
            dto.setRegistrationCreatedAt(r.getRegistrationCreatedAt());
            dto.setRegistrationCompletedAt(r.getRegistrationCompletedAt());

            PaymentReceipt receipt = r.getPaymentReceipt();
            if (receipt != null) {
                dto.setReceiptId(receipt.getId());
                dto.setPaid(receipt.getPaid());
                dto.setVerified(receipt.getVerified());
                dto.setFileName(receipt.getFileName());
                dto.setFilePath(receipt.getFilePath());
                dto.setYandexDiskUrl(receipt.getYandexDiskUrl());
            }

            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/registrations")
    public ResponseEntity<List<ExportRegistrationDTO>> getAllRegistrations() {
        return getPaidRegistrations();
    }

    @PostMapping("/receipts/{receiptId}/yandex")
    public ResponseEntity<?> updateReceiptYandexUrl(@PathVariable Long receiptId, @RequestBody ExportUploadRequest request) {
        return paymentReceiptRepository.findById(receiptId).map(r -> {
            r.setYandexDiskUrl(request.getYandexUrl());
            r.setYandexDiskUploaded(true);
            paymentReceiptRepository.save(r);
            return ResponseEntity.ok().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Data
    public static class ExportUploadRequest {
        private String yandexUrl;
    }
}
