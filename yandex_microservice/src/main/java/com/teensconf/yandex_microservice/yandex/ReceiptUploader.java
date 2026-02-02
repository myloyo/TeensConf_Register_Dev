package com.teensconf.yandex_microservice.yandex;

import com.teensconf.yandex_microservice.dto.ExportRegistrationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReceiptUploader {

    private final YandexClient yandexClient;

    @Value("${app.uploads.receipts-dir:./uploads/receipts}")
    private String receiptsBaseDir;

    public void uploadReceiptsAndNotify(List<ExportRegistrationDTO> registrations, String receiptsFolder, String backendBase) {
        for (ExportRegistrationDTO r : registrations) {
            try {
                Long receiptId = r.getReceiptId();
                if (receiptId == null) {
                    log.warn("No receiptId for registrationId={}", r.getId());
                    continue;
                }

                String fileName = extractFileName(r);
                if (fileName == null || fileName.isEmpty()) {
                    log.warn("Cannot determine filename for receiptId={}", receiptId);
                    continue;
                }

                Path filePath = Paths.get(receiptsBaseDir, fileName);
                if (!Files.exists(filePath) && r.getFilePath() != null) {
                    Path fullPath = Paths.get(r.getFilePath());
                    if (Files.exists(fullPath)) {
                        filePath = fullPath;
                    } else {
                        String simpleFileName = fullPath.getFileName().toString();
                        filePath = Paths.get(receiptsBaseDir, simpleFileName);
                    }
                }

                if (!Files.exists(filePath)) {
                    log.warn("Receipt file not found: {} (searched in: {})", fileName, filePath.toAbsolutePath());
                    continue;
                }

                byte[] fileContent = Files.readAllBytes(filePath);
                String diskPath = receiptsFolder + "/" + filePath.getFileName().toString();

                boolean uploaded = yandexClient.uploadFile(diskPath, fileContent);
                if (!uploaded) {
                    log.error("Failed to upload receipt {} to Yandex Disk", receiptId);
                    continue;
                }

                Thread.sleep(1000); // маленькая пауза на стабилизацию

                String publicUrl = yandexClient.getPublicUrl(diskPath);
                if (publicUrl == null || publicUrl.isEmpty() || "null".equals(publicUrl)) {
                    log.warn("Public URL is null/empty, trying alternative for receiptId={}", receiptId);
                    publicUrl = yandexClient.getAlternativePublicUrl(diskPath);
                    if (publicUrl == null || publicUrl.isEmpty()) {
                        log.error("Failed to get any URL for receiptId={}", receiptId);
                        continue;
                    }
                }

                notifyBackend(receiptId, publicUrl, backendBase);

            } catch (Exception e) {
                log.error("Error uploading receipt for receiptId {}: {}", r.getReceiptId(), e.getMessage(), e);
            }
        }
    }

    private void notifyBackend(Long receiptId, String publicUrl, String backendBase) {
        try {
            String url = backendBase + "/api/internal/export/receipts/" + receiptId + "/yandex";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> body = new HashMap<>();
            body.put("yandexUrl", publicUrl);
            HttpEntity<Map<String, String>> ent = new HttpEntity<>(body, headers);
            new RestTemplate().postForEntity(url, ent, Void.class);
            log.info("Notified backend about receipt {} -> {}", receiptId, publicUrl);
        } catch (Exception ex) {
            log.error("Failed to notify backend for receipt {}: {}", receiptId, ex.getMessage());
        }
    }

    private String extractFileName(ExportRegistrationDTO r) {
        if (r.getFilePath() != null) {
            Path path = Paths.get(r.getFilePath());
            return path.getFileName().toString();
        }
        if (r.getId() != null && r.getFirstName() != null && r.getLastName() != null) {
            return r.getId() + "_" + r.getFirstName() + "_" + r.getLastName() + "_receipt.pdf";
        }
        return null;
    }
}
