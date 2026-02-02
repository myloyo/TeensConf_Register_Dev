package com.teensconf.service;

import com.teensconf.entity.PaymentReceipt;
import com.teensconf.entity.Registration;
import com.teensconf.repository.RegistrationRepository;
import com.teensconf.repository.PaymentReceiptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class YandexSheetsService {

    private final RestTemplate restTemplate;
    private final RegistrationRepository registrationRepository;
    private final PaymentReceiptRepository paymentReceiptRepository; // Добавляем репозиторий для сохранения статуса

    @Value("${yandex.sheets.access-token}")
    private String accessToken;

    @Value("${yandex.sheets.file-name}")
    private String diskFilePath;

    @Value("${yandex.sheets.receipts-folder:/Teens_Conf_Receipts}")
    private String receiptsFolder;

    @Value("${yandex.sheets.upload-receipt:true}")
    private boolean uploadReceiptsEnabled;

    @Scheduled(fixedDelay = 300000, initialDelay = 10000)
    public void uploadRegistrationsToDisk() {
        try {
            List<Registration> registrations = registrationRepository.findAll();

            if (uploadReceiptsEnabled) {
                uploadReceiptsToDisk(registrations);
            }

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

    private void uploadReceiptsToDisk(List<Registration> registrations) {
        for (Registration registration : registrations) {
            try {
                PaymentReceipt receipt = registration.getPaymentReceipt();
                if (receipt != null && receipt.getFilePath() != null &&
                        !Boolean.TRUE.equals(receipt.getYandexDiskUploaded())) {

                    boolean success = uploadReceiptToDisk(registration, receipt);
                    if (success) {

                        paymentReceiptRepository.save(receipt);
                    }
                }
            } catch (Exception e) {
                log.error("Error uploading receipt for registration {}: {}",
                        registration.getId(), e.getMessage());
            }
        }
    }

    private boolean uploadReceiptToDisk(Registration registration, PaymentReceipt receipt) {
        try {
            Path filePath = Path.of(receipt.getFilePath());
            if (!Files.exists(filePath)) {
                log.warn("Receipt file not found: {}", receipt.getFilePath());
                return false;
            }

            byte[] fileContent = Files.readAllBytes(filePath);

            String filename = generateReceiptFileName(registration, receipt.getFileName());
            String diskPath = receiptsFolder + "/" + filename;

            boolean uploaded = uploadFileWithRetry(diskPath, fileContent);
            if (uploaded) {
                String publicUrl = getFilePublicUrl(diskPath);

                receipt.setYandexDiskUrl(publicUrl);
                receipt.setYandexDiskUploaded(true);

                log.info("Receipt for registration {} uploaded to Yandex.Disk: {}",
                        registration.getId(), publicUrl);
                return true;
            }
        } catch (IOException e) {
            log.error("Error reading receipt file for registration {}: {}",
                    registration.getId(), e.getMessage());
        }
        return false;
    }

    /**
     * Генерация имени файла в том же формате, что и в PaymentService
     */
    private String generateReceiptFileName(Registration registration, String originalFileName) {
        String firstNameTranslit = transliterate(registration.getFirstName());
        String lastNameTranslit = transliterate(registration.getLastName());
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        String fileExtension = ".pdf"; // По умолчанию для чеков
        if (originalFileName != null) {
            int lastDotIndex = originalFileName.lastIndexOf(".");
            if (lastDotIndex > 0) {
                fileExtension = originalFileName.substring(lastDotIndex);
            }
        }

        return String.format("%d_%s_%s_%s%s",
                registration.getId(),
                firstNameTranslit,
                lastNameTranslit,
                timestamp,
                fileExtension.toLowerCase());
    }

    /**
     * Транслитерация кириллицы в латиницу
     */
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

    private String getFilePublicUrl(String diskPath) {
        try {
            publishFile(diskPath);

            Thread.sleep(2000);

            String encodedPath = URLEncoder.encode(diskPath, StandardCharsets.UTF_8);
            String url = "https://cloud-api.yandex.net/v1/disk/resources?path=" + encodedPath;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "OAuth " + accessToken);
            headers.set("Accept", "application/json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<FileInfo> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, FileInfo.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                FileInfo fileInfo = response.getBody();

                if (fileInfo.getPublicKey() != null && !fileInfo.getPublicKey().equals("null")) {
                    return "https://yadi.sk/i/" + fileInfo.getPublicKey();
                } else {
                    log.warn("Public key is null for file: {}, trying alternative method", diskPath);
                    return getAlternativePublicUrl(diskPath);
                }
            }
        } catch (Exception e) {
            log.error("Error getting public URL for file {}: {}", diskPath, e.getMessage());
        }
        return null;
    }

    private String getAlternativePublicUrl(String diskPath) {
        try {
            String encodedPath = URLEncoder.encode(diskPath, StandardCharsets.UTF_8);
            String url = "https://cloud-api.yandex.net/v1/disk/resources?path=" + encodedPath + "&fields=public_url";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "OAuth " + accessToken);
            headers.set("Accept", "application/json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<FileInfoExtended> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, FileInfoExtended.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                FileInfoExtended fileInfo = response.getBody();
                if (fileInfo.getPublicUrl() != null) {
                    return fileInfo.getPublicUrl();
                }
            }

            return "https://disk.yandex.ru/client/disk" + diskPath;

        } catch (Exception e) {
            log.error("Error getting alternative public URL for file {}: {}", diskPath, e.getMessage());
            return "https://disk.yandex.ru/client/disk" + diskPath;
        }
    }

    private void publishFile(String diskPath) {
        try {
            String encodedPath = URLEncoder.encode(diskPath, StandardCharsets.UTF_8);
            String url = "https://cloud-api.yandex.net/v1/disk/resources/publish?path=" + encodedPath;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "OAuth " + accessToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<PublishResponse> response = restTemplate.exchange(
                    url, HttpMethod.PUT, entity, PublishResponse.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.debug("File published successfully: {}", diskPath);
            } else {
                log.warn("Failed to publish file: {}, status: {}", diskPath, response.getStatusCode());
            }

        } catch (Exception e) {
            log.warn("Error publishing file {}: {}", diskPath, e.getMessage());
        }
    }

    private byte[] buildSimplifiedXlsx(List<Registration> registrations) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Registrations");

            String[] columns = {
                    "id", "Имя", "Фамилия", "Email", "Дата рождения", "Телефон",
                    "Город", "Нужно жилье", "Церковь", "Роль", "ФИО родителя",
                    "Телефон родителя", "ID оплаты", "Ссылка на чек"
            };

            Row header = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            // Создаем стиль для ссылок один раз
            CellStyle linkStyle = workbook.createCellStyle();
            Font linkFont = workbook.createFont();
            linkFont.setUnderline(Font.U_SINGLE);
            linkFont.setColor(IndexedColors.BLUE.getIndex());
            linkStyle.setFont(linkFont);

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
                row.createCell(6).setCellValue(r.getTelegram());
                row.createCell(7).setCellValue(r.getCity());
                row.createCell(8).setCellValue(Boolean.TRUE.equals(r.getNeedAccommodation()) ? "Да" : "Нет");
                row.createCell(9).setCellValue(r.getChurch());
                row.createCell(10).setCellValue(r.getRole());
                row.createCell(11).setCellValue(r.getParentFullName() != null ? r.getParentFullName() : "");
                row.createCell(12).setCellValue(r.getParentPhone() != null ? r.getParentPhone() : "");

                String paymentId = r.getPaymentReceipt() != null ? r.getPaymentReceipt().getId().toString() : "";
                row.createCell(13).setCellValue(paymentId);

                String receiptUrl = r.getPaymentReceipt() != null &&
                        Boolean.TRUE.equals(r.getPaymentReceipt().getYandexDiskUploaded())
                        ? r.getPaymentReceipt().getYandexDiskUrl()
                        : "Не загружен";

                Cell receiptCell = row.createCell(14);

                if (!"Не загружен".equals(receiptUrl) && receiptUrl != null && !receiptUrl.contains("null")) {
                    receiptCell.setCellValue("Ссылка на чек");
                    try {
                        Hyperlink link = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
                        link.setAddress(receiptUrl);
                        receiptCell.setHyperlink(link);
                        receiptCell.setCellStyle(linkStyle);
                    } catch (Exception e) {
                        log.warn("Не удалось создать гиперссылку для: {}", receiptUrl);
                        receiptCell.setCellValue(receiptUrl);
                    }
                } else {
                    receiptCell.setCellValue("Не загружен");
                }
            }
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
        }
    }

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
                    URLEncoder.encode(path, StandardCharsets.UTF_8) + "&overwrite=true";

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

    private static class FileInfo {
        private String public_key;
        public String getPublicKey() { return public_key; }
        public void setPublicKey(String publicKey) { this.public_key = publicKey; }
    }

    private static class FileInfoExtended {
        private String public_url;
        public String getPublicUrl() { return public_url; }
        public void setPublicUrl(String publicUrl) { this.public_url = publicUrl; }
    }

    private static class PublishResponse {
        private String href;
        private String method;
        private boolean templated;

        public String getHref() { return href; }
        public void setHref(String href) { this.href = href; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public boolean isTemplated() { return templated; }
        public void setTemplated(boolean templated) { this.templated = templated; }
    }
}