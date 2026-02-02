package com.teensconf.yandex_microservice.service;

import com.teensconf.yandex_microservice.dto.ExportRegistrationDTO;
import com.teensconf.yandex_microservice.dto.RegistrationEventDTO;
import com.teensconf.yandex_microservice.yandex.YandexClient;
import com.teensconf.yandex_microservice.yandex.ReceiptUploader;
import com.teensconf.yandex_microservice.yandex.SheetFormatter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class YandexExportService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final YandexClient yandexClient;
    private final SheetFormatter sheetFormatter;
    private final ReceiptUploader receiptUploader;
    private final RedisCacheService redisCacheService;

    @Value("${backend.base-url:http://backend:8080}")
    private String backendBase;

    @Value("${yandex.sheets.file-name:/Teens_Conf/registrations.xlsx}")
    private String diskFilePath;

    @Value("${yandex.sheets.receipts-folder:/Teens_Conf_Receipts}")
    private String receiptsFolder;

    @Value("${yandex.sheets.upload-receipt:true}")
    private boolean uploadReceiptsEnabled;

    // Ключи для хэшей
    private static final String HASH_ALL_KEY = "yandex:hash:all";
    private static final String HASH_PAID_KEY = "yandex:hash:paid";

    @Scheduled(fixedDelayString = "${yandex.sheets.refresh-ms:600000}", initialDelay = 15000)
    public void scheduledExport() {
        log.info("Scheduled export triggered");

        try {
            // 1. Проверяем, есть ли изменения в БД
            String currentAllHash = calculateAllRegistrationsHash();
            String currentPaidHash = calculatePaidRegistrationsHash();

            String cachedAllHash = redisCacheService.getHash(HASH_ALL_KEY);
            String cachedPaidHash = redisCacheService.getHash(HASH_PAID_KEY);

            boolean allChanged = !currentAllHash.equals(cachedAllHash);
            boolean paidChanged = !currentPaidHash.equals(cachedPaidHash);

            if (allChanged || paidChanged) {
                log.info("Changes detected in DB, performing export");

                // 2. Получаем свежие данные из БД
                List<ExportRegistrationDTO> all = fetchAllRegistrationsFromBackend();
                List<ExportRegistrationDTO> paid = fetchPaidRegistrationsFromBackend();

                // 3. Кэшируем данные в Redis
                redisCacheService.cacheAllRegistrations(all);
                redisCacheService.cachePaidRegistrations(paid);

                // 4. Сохраняем новые хэши
                redisCacheService.saveHash(HASH_ALL_KEY, currentAllHash);
                redisCacheService.saveHash(HASH_PAID_KEY, currentPaidHash);

                // 5. Выполняем экспорт
                doExport(paid, all);
            } else {
                log.info("No changes in DB, exporting from cache");

                // Берем данные из кэша
                List<ExportRegistrationDTO> all = redisCacheService.getAllRegistrationsFromCache();
                List<ExportRegistrationDTO> paid = redisCacheService.getPaidRegistrationsFromCache();

                if (all == null || paid == null) {
                    // Если кэш пустой, загружаем заново
                    all = fetchAllRegistrationsFromBackend();
                    paid = fetchPaidRegistrationsFromBackend();

                    redisCacheService.cacheAllRegistrations(all);
                    redisCacheService.cachePaidRegistrations(paid);
                }

                doExport(paid, all);
            }
        } catch (Exception e) {
            log.error("scheduledExport failed", e);
        }
    }

    public void handleEvent(RegistrationEventDTO event) {
        log.info("Registration event received: id={}, type={}",
                event.getRegistrationId(), event.getEventType());

        redisCacheService.clearCache();

        try {
            List<ExportRegistrationDTO> all = fetchAllRegistrationsFromBackend();
            List<ExportRegistrationDTO> paid = fetchPaidRegistrationsFromBackend();

            String allHash = calculateHash(all);
            String paidHash = calculateHash(paid);

            redisCacheService.cacheAllRegistrations(all);
            redisCacheService.cachePaidRegistrations(paid);
            redisCacheService.saveHash(HASH_ALL_KEY, allHash);
            redisCacheService.saveHash(HASH_PAID_KEY, paidHash);

            doExport(paid, all);
        } catch (Exception e) {
            log.error("handleEvent export failed", e);
        }
    }


    public void forceCacheRefresh() {
        log.info("Forcing cache refresh");
        redisCacheService.clearCache();
    }

    private void doExport(List<ExportRegistrationDTO> paid, List<ExportRegistrationDTO> all) throws Exception {
        if (paid == null) paid = new ArrayList<>();
        if (all == null) all = new ArrayList<>();

        log.info("Exporting data: paid={}, all={}", paid.size(), all.size());

        if (uploadReceiptsEnabled) {
            List<ExportRegistrationDTO> toUpload = new ArrayList<>();
            for (ExportRegistrationDTO r : paid) {
                if (r.getFilePath() != null && r.getReceiptId() != null &&
                        (r.getYandexDiskUrl() == null || r.getYandexDiskUrl().isEmpty())) {
                    toUpload.add(r);
                }
            }
            if (!toUpload.isEmpty()) {
                log.info("Uploading {} receipts to Yandex.Disk", toUpload.size());
                receiptUploader.uploadReceiptsAndNotify(toUpload, receiptsFolder, backendBase);
            }
        }

        byte[] xlsx = sheetFormatter.buildXlsxWithTwoSheets(paid, all);
        boolean ok = yandexClient.uploadFile(diskFilePath, xlsx);
        if (ok) {
            log.info("Export XLSX uploaded: paid={} total={}", paid.size(), all.size());
        } else {
            log.warn("Failed to upload export XLSX");
        }
    }


    private List<ExportRegistrationDTO> fetchAllRegistrationsFromBackend() {
        return fetchExportList("/api/internal/export/registrations");
    }


    private List<ExportRegistrationDTO> fetchPaidRegistrationsFromBackend() {
        return fetchExportList("/api/internal/export/paid-registrations");
    }

    private List<ExportRegistrationDTO> fetchExportList(String path) {
        try {
            String url = backendBase + path;
            log.debug("Fetching from URL: {}", url);

            String json = restTemplate.getForObject(url, String.class);
            if (json == null || json.trim().isEmpty()) {
                log.warn("Empty response from {}", path);
                return new ArrayList<>();
            }

            return objectMapper.readValue(json, new TypeReference<List<ExportRegistrationDTO>>(){});
        } catch (Exception e) {
            log.error("Failed to fetch {}: {}", path, e.getMessage());
            return new ArrayList<>();
        }
    }


    private String calculateAllRegistrationsHash() {
        try {
            List<ExportRegistrationDTO> registrations = fetchAllRegistrationsFromBackend();
            return calculateHash(registrations);
        } catch (Exception e) {
            log.error("Failed to calculate hash for all registrations", e);
            return "error";
        }
    }


    private String calculatePaidRegistrationsHash() {
        try {
            List<ExportRegistrationDTO> registrations = fetchPaidRegistrationsFromBackend();
            return calculateHash(registrations);
        } catch (Exception e) {
            log.error("Failed to calculate hash for paid registrations", e);
            return "error";
        }
    }


    private String calculateHash(List<ExportRegistrationDTO> registrations) {
        if (registrations == null || registrations.isEmpty()) {
            return "empty";
        }

        // Создаем хэш на основе ID и времени последнего изменения
        StringBuilder sb = new StringBuilder();
        for (ExportRegistrationDTO reg : registrations) {
            sb.append(reg.getId()).append(":");
            // Если есть поле с временем обновления, можно добавить его
            // sb.append(reg.getUpdatedAt() != null ? reg.getUpdatedAt().toEpochMilli() : "0").append("|");
        }
        return Integer.toHexString(sb.toString().hashCode());
    }


    public List<ExportRegistrationDTO> getAllRegistrationsFromCache() {
        return redisCacheService.getAllRegistrationsFromCache();
    }

    public List<ExportRegistrationDTO> getPaidRegistrationsFromCache() {
        return redisCacheService.getPaidRegistrationsFromCache();
    }
}