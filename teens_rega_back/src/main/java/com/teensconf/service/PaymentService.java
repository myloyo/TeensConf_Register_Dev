package com.teensconf.service;

import com.teensconf.dto.PaymentCompletionRequest;
import com.teensconf.entity.PaymentReceipt;
import com.teensconf.entity.Registration;
import com.teensconf.exception.FileFormatException;
import com.teensconf.exception.NotFoundException;
import com.teensconf.exception.StorageException;
import com.teensconf.exception.ValidationException;
import com.teensconf.repository.PaymentReceiptRepository;
import com.teensconf.repository.RegistrationRepository;
import com.teensconf.util.TransliterationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.teensconf.kafka.EventProducer;
import com.teensconf.dto.events.PaymentCompletedEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RegistrationRepository registrationRepository;
    private final PaymentReceiptRepository paymentReceiptRepository;
    private final PdfValidationService pdfValidationService;
    private final EmailService emailService;
    private final EventProducer eventProducer;

    @Value("${app.upload.dir:./uploads/receipts}")
    String uploadDir;

    @Transactional
    public PaymentReceipt processPaymentCompletion(Long registrationId, PaymentCompletionRequest request) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new NotFoundException("Регистрация не найдена"));

        if (registration.getRegistrationCompletedAt() != null) {
            throw new ValidationException("Регистрация уже завершена");
        }

        PaymentReceipt receipt = new PaymentReceipt();
        receipt.setRegistration(registration);

        boolean isValid;

        if (request.getReceiptFile() != null && !request.getReceiptFile().isEmpty()) {
            isValid = processReceiptFile(request.getReceiptFile(), receipt);
        } else {
            throw new ValidationException("Не предоставлены данные об оплате");
        }

        receipt.setVerified(isValid);
        receipt.setPaid(isValid);

        PaymentReceipt savedReceipt = paymentReceiptRepository.save(receipt);

        if (isValid) {
            completeRegistration(registration);
        }

        try {
            PaymentCompletedEvent evt = new PaymentCompletedEvent(
                    registration.getId(), savedReceipt.getId(), savedReceipt.getPaid() != null && savedReceipt.getPaid(), savedReceipt.getVerified() != null && savedReceipt.getVerified()
            );
            eventProducer.publish("registrations.events", registration.getId().toString(), evt);
        } catch (Exception e) {
            log.warn("Не удалось опубликовать событие об оплате: {}", e.getMessage());
        }

        return savedReceipt;
    }

    private boolean processReceiptFile(MultipartFile file, PaymentReceipt receipt) {
        try {
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || !originalFileName.toLowerCase().endsWith(".pdf")) {
                throw new FileFormatException("Файл должен быть в формате PDF");
            }

            byte[] fileBytes = file.getBytes();
            PdfValidationService.ValidationResult validationResult = pdfValidationService.validatePdf(fileBytes);

            if (!validationResult.isValid()) {
                throw new FileFormatException(validationResult.getErrorMessage());
            }

            Registration registration = receipt.getRegistration();
            String fileName = generateReceiptFileName(registration, originalFileName);
            Path filePath = Paths.get(uploadDir, fileName);

            Files.createDirectories(Paths.get(uploadDir));
            Files.write(filePath, fileBytes);

            receipt.setFileName(originalFileName);
            receipt.setFilePath(filePath.toString());

            return true;

        } catch (IOException e) {
            log.error("Error saving receipt file", e);
            throw new StorageException("Ошибка при сохранении файла чека: " + e.getMessage(), e);
        }
    }

    private String generateReceiptFileName(Registration registration, String originalFileName) {
        String firstNameTranslit = TransliterationUtil.transliterate(registration.getFirstName());
        String lastNameTranslit = TransliterationUtil.transliterate(registration.getLastName());
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        String fileExtension = ".pdf";
        if (originalFileName != null) {
            int lastDotIndex = originalFileName.lastIndexOf('.');
            if (lastDotIndex > 0) {
                fileExtension = originalFileName.substring(lastDotIndex);
            }
        }

        return String.format("%d_%s_%s_%s%s",
                registration.getId(),
                firstNameTranslit,
                lastNameTranslit,
                timestamp,
                fileExtension);
    }

    private void completeRegistration(Registration registration) {
        registration.setRegistrationCompletedAt(LocalDateTime.now());
        registrationRepository.save(registration);

        try {
            emailService.sendPaymentSuccessNotification(registration);
        } catch (Exception e) {
            log.error("Ошибка отправки уведомления об успешной оплате: {}", e.getMessage());
        }

        log.info("Регистрация завершена: {}", registration.getEmail());
    }

    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            log.error("Could not create upload directory", e);
        }
    }
}