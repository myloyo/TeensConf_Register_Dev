package com.teensconf.service;

import com.teensconf.dto.PaymentCompletionRequest;
import com.teensconf.entity.PaymentReceipt;
import com.teensconf.entity.Registration;
import com.teensconf.repository.PaymentReceiptRepository;
import com.teensconf.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RegistrationRepository registrationRepository;
    private final PaymentReceiptRepository paymentReceiptRepository;
    private final EmailService emailService;

    @Value("${app.upload.dir:./uploads/receipts}")
    String uploadDir;

    private static final String REFERENCE_SUFFIX = "0011630701";

    @Transactional
    public PaymentReceipt processPaymentCompletion(Long registrationId, PaymentCompletionRequest request) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Регистрация не найдена"));

        if (registration.getRegistrationCompletedAt() != null) {
            throw new IllegalArgumentException("Регистрация уже завершена");
        }

        PaymentReceipt receipt = new PaymentReceipt();
        receipt.setRegistration(registration);
        receipt.setDonationAmount(500.0);

        boolean isValid = false;

        if (request.getPaymentReference() != null && !request.getPaymentReference().trim().isEmpty()) {
            isValid = processPaymentReference(request.getPaymentReference(), receipt);
        } else if (request.getReceiptFile() != null && !request.getReceiptFile().isEmpty()) {
            isValid = processReceiptFile(request.getReceiptFile(), receipt);
        } else {
            throw new IllegalArgumentException("Не предоставлены данные об оплате");
        }

        receipt.setVerified(isValid);
        receipt.setPaid(isValid);

        PaymentReceipt savedReceipt = paymentReceiptRepository.save(receipt);

        // Если верификация успешна - завершаем регистрацию
        if (isValid) {
            completeRegistration(registration);
        }

        return savedReceipt;
    }

    private boolean processPaymentReference(String reference, PaymentReceipt receipt) {
        receipt.setPaymentReference(reference);
        return isValidReference(reference);
    }

    private boolean processReceiptFile(MultipartFile file, PaymentReceipt receipt) {
        try {
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || !originalFileName.toLowerCase().endsWith(".pdf")) {
                return false;
            }

            String fileName = UUID.randomUUID() + ".pdf";
            Path filePath = Paths.get(uploadDir, fileName);

            Files.createDirectories(Paths.get(uploadDir));
            Files.write(filePath, file.getBytes());

            receipt.setFileName(originalFileName);
            receipt.setFilePath(filePath.toString());
            receipt.setFileSize(file.getSize());

            return true;

        } catch (IOException e) {
            log.error("Error saving receipt file", e);
            return false;
        }
    }

    boolean isValidReference(String reference) {
        return reference != null &&
                reference.length() == 32 &&
                reference.endsWith(REFERENCE_SUFFIX);
    }

    private void completeRegistration(Registration registration) {
        registration.setRegistrationCompletedAt(LocalDateTime.now());
        registrationRepository.save(registration);

        emailService.sendPaymentSuccessNotification(registration);
        log.info("Registration completed: {}", registration.getEmail());
    }

    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            log.error("Could not create upload directory", e);
        }
    }
}