package com.teensconf.service;

import com.teensconf.entity.Registration;
import com.teensconf.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class YandexSheetsService {

    private final RestTemplate restTemplate;
    private final RegistrationRepository registrationRepository;

    @Value("${yandex.sheets.access-token}")
    private String accessToken;

    @Value("${yandex.sheets.file-name}")
    private String diskFilePath;

    // Увеличиваем интервал - не нужно так часто синхронизировать
    @Scheduled(fixedDelay = 300000, initialDelay = 10000)
    public void uploadRegistrationsToDisk() {
        try {
            List<Registration> registrations = registrationRepository.findAll();
            byte[] xlsxBytes = buildSimplifiedXlsx(registrations);

            boolean uploaded = uploadFileWithRetry(diskFilePath, xlsxBytes);
            if (uploaded) {
                log.info("Uploaded simplified XLSX with {} registrations", registrations.size());
            } else {
                log.warn("Failed to upload XLSX to Yandex.Disk");
            }
        } catch (Exception e) {
            log.error("Error during upload", e);
        }
    }

    private byte[] buildSimplifiedXlsx(List<Registration> registrations) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Registrations");

            // ТОЛЬКО нужные поля
            String[] columns = {
                    "id", "Имя", "Фамилия", "Email", "Дата рождения", "Телефон",
                    "Город", "Нужно жилье", "Церковь", "Роль", "ФИО родителя",
                    "Телефон родителя", "ID оплаты"
            };

            // Заголовок
            Row header = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            // Данные
            int rowNum = 1;
            for (Registration r : registrations) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.getId());
                row.createCell(1).setCellValue(r.getFirstName());
                row.createCell(2).setCellValue(r.getLastName());
                row.createCell(3).setCellValue(r.getEmail());
                row.createCell(4).setCellValue(r.getBirthDate());
                row.createCell(5).setCellValue(r.getPhone());
                row.createCell(6).setCellValue(r.getCity());
                row.createCell(7).setCellValue(Boolean.TRUE.equals(r.getNeedAccommodation()) ? "Да" : "Нет");
                row.createCell(8).setCellValue(r.getChurch());
                row.createCell(9).setCellValue(r.getRole());
                row.createCell(10).setCellValue(r.getParentFullName() != null ? r.getParentFullName() : "");
                row.createCell(11).setCellValue(r.getParentPhone() != null ? r.getParentPhone() : "");

                // ID оплаты
                String paymentId = r.getPaymentReceipt() != null ? r.getPaymentReceipt().getId().toString() : "";
                row.createCell(12).setCellValue(paymentId);
            }

            // Авторазмер
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    // Остальные методы без изменений
    private boolean uploadFileWithRetry(String path, byte[] content) {
        int retries = 3;
        while (retries-- > 0) {
            try {
                return uploadFile(path, content);
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.LOCKED) {
                    log.warn("File locked, retrying...");
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                } else {
                    log.error("Error uploading file", e);
                    return false;
                }
            }
        }
        return false;
    }

    private boolean uploadFile(String path, byte[] content) {
        try {
            if (!path.startsWith("/")) {
                path = "/" + path;
            }

            String uploadUrl = "https://cloud-api.yandex.net/v1/disk/resources/upload?path=" +
                    java.net.URLEncoder.encode(path, StandardCharsets.UTF_8) + "&overwrite=true";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "OAuth " + accessToken);
            headers.set("Accept", "application/json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<UploadHref> response = restTemplate.exchange(
                    uploadUrl, HttpMethod.GET, entity, UploadHref.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                return false;
            }

            String href = response.getBody().getHref();

            HttpHeaders putHeaders = new HttpHeaders();
            putHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            putHeaders.setContentLength(content.length);

            HttpEntity<byte[]> putEntity = new HttpEntity<>(content, putHeaders);
            ResponseEntity<String> putResponse = restTemplate.exchange(
                    href, HttpMethod.PUT, putEntity, String.class);

            return putResponse.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.error("Error uploading to Yandex.Disk", e);
            return false;
        }
    }

    private static class UploadHref {
        private String href;
        public String getHref() { return href; }
        public void setHref(String href) { this.href = href; }
    }
}