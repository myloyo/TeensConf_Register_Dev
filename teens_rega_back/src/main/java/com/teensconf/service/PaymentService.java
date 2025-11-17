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
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RegistrationRepository registrationRepository;
    private final PaymentReceiptRepository paymentReceiptRepository;
    private final EmailService emailService;
    private final PdfValidationService pdfValidationService;

    @Value("${app.upload.dir:./uploads/receipts}")
    String uploadDir;

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

        if (request.getReceiptFile() != null && !request.getReceiptFile().isEmpty()) {
            isValid = processReceiptFile(request.getReceiptFile(), receipt);
        } else {
            throw new IllegalArgumentException("Не предоставлены данные об оплате");
        }

        receipt.setVerified(isValid);
        receipt.setPaid(isValid);

        PaymentReceipt savedReceipt = paymentReceiptRepository.save(receipt);

        if (isValid) {
            completeRegistration(registration);
        }

        return savedReceipt;
    }

    private boolean processReceiptFile(MultipartFile file, PaymentReceipt receipt) {
        try {
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || !originalFileName.toLowerCase().endsWith(".pdf")) {
                throw new IllegalArgumentException("Файл должен быть в формате PDF");
            }

            byte[] fileBytes = file.getBytes();
            PdfValidationService.ValidationResult validationResult = pdfValidationService.validatePdf(fileBytes);

            if (!validationResult.isValid()) {
                throw new IllegalArgumentException(validationResult.getErrorMessage());
            }

            Registration registration = receipt.getRegistration();
            String fileName = generateReceiptFileName(registration, originalFileName);
            Path filePath = Paths.get(uploadDir, fileName);

            Files.createDirectories(Paths.get(uploadDir));
            Files.write(filePath, fileBytes);

            receipt.setFileName(originalFileName); // Сохраняем оригинальное имя
            receipt.setFilePath(filePath.toString());
            receipt.setFileSize(file.getSize());

            return true;

        } catch (IOException e) {
            log.error("Error saving receipt file", e);
            throw new IllegalArgumentException("Ошибка при сохранении файла чека: " + e.getMessage());
        }
    }

    private String generateReceiptFileName(Registration registration, String originalFileName) {
        String firstNameTranslit = transliterate(registration.getFirstName());
        String lastNameTranslit = transliterate(registration.getLastName());
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        String fileExtension = "";
        int lastDotIndex = originalFileName.lastIndexOf(".");
        if (lastDotIndex > 0) {
            fileExtension = originalFileName.substring(lastDotIndex);
        } else {
            fileExtension = ".pdf";
        }

        return String.format("%d_%s_%s_%s%s",
                registration.getId(),
                firstNameTranslit,
                lastNameTranslit,
                timestamp,
                fileExtension);
    }

    private String transliterate(String text) {
        if (text == null) return "";

        Map<Character, String> translitMap = new HashMap<>();
        translitMap.put('а', "a"); translitMap.put('б', "b"); translitMap.put('в', "v");
        translitMap.put('г', "g"); translitMap.put('д', "d"); translitMap.put('е', "e");
        translitMap.put('ё', "e"); translitMap.put('ж', "zh"); translitMap.put('з', "z");
        translitMap.put('и', "i"); translitMap.put('й', "y"); translitMap.put('к', "k");
        translitMap.put('л', "l"); translitMap.put('м', "m"); translitMap.put('н', "n");
        translitMap.put('о', "o"); translitMap.put('п', "p"); translitMap.put('р', "r");
        translitMap.put('с', "s"); translitMap.put('т', "t"); translitMap.put('у', "u");
        translitMap.put('ф', "f"); translitMap.put('х', "h"); translitMap.put('ц', "ts");
        translitMap.put('ч', "ch"); translitMap.put('ш', "sh"); translitMap.put('щ', "sch");
        translitMap.put('ы', "y"); translitMap.put('э', "e"); translitMap.put('ю', "yu");
        translitMap.put('я', "ya");
        translitMap.put('А', "A"); translitMap.put('Б', "B"); translitMap.put('В', "V");
        translitMap.put('Г', "G"); translitMap.put('Д', "D"); translitMap.put('Е', "E");
        translitMap.put('Ё', "E"); translitMap.put('Ж', "Zh"); translitMap.put('З', "Z");
        translitMap.put('И', "I"); translitMap.put('Й', "Y"); translitMap.put('К', "K");
        translitMap.put('Л', "L"); translitMap.put('М', "M"); translitMap.put('Н', "N");
        translitMap.put('О', "O"); translitMap.put('П', "P"); translitMap.put('Р', "R");
        translitMap.put('С', "S"); translitMap.put('Т', "T"); translitMap.put('У', "U");
        translitMap.put('Ф', "F"); translitMap.put('Х', "H"); translitMap.put('Ц', "Ts");
        translitMap.put('Ч', "Ch"); translitMap.put('Ш', "Sh"); translitMap.put('Щ', "Sch");
        translitMap.put('Ы', "Y"); translitMap.put('Э', "E"); translitMap.put('Ю', "Yu");
        translitMap.put('Я', "Ya");

        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (translitMap.containsKey(c)) {
                result.append(translitMap.get(c));
            } else if (Character.isLetterOrDigit(c)) {
                result.append(c);
            } else {
                result.append('_');
            }
        }

        return result.toString().replaceAll("_+", "_").replaceAll("^_|_$", "");
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