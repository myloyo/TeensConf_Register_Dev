package com.teensconf.yandex_microservice.yandex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class YandexClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${yandex.sheets.access-token}")
    private String accessToken;

    private static final String BASE_URL = "https://cloud-api.yandex.net/v1/disk";

    public boolean uploadFile(String path, byte[] content) {
        try {
            if (!path.startsWith("/")) path = "/" + path;

            String uploadUrl = BASE_URL + "/resources/upload?path=" +
                    URLEncoder.encode(path, StandardCharsets.UTF_8) + "&overwrite=true";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "OAuth " + accessToken);
            headers.set("Accept", "application/json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> uploadResponse = restTemplate.exchange(
                    uploadUrl, HttpMethod.GET, entity, String.class);

            if (uploadResponse.getStatusCode() != HttpStatus.OK) {
                log.error("Failed to get upload URL: {}", uploadResponse.getStatusCode());
                return false;
            }

            JsonNode uploadNode = objectMapper.readTree(uploadResponse.getBody());
            String href = uploadNode.get("href").asText();

            if (href == null || href.isEmpty()) {
                log.error("No upload href in response");
                return false;
            }

            HttpHeaders putHeaders = new HttpHeaders();
            putHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            putHeaders.setContentLength(content.length);

            HttpEntity<byte[]> putEntity = new HttpEntity<>(content, putHeaders);

            ResponseEntity<String> putResponse = restTemplate.exchange(
                    href, HttpMethod.PUT, putEntity, String.class);

            boolean success = putResponse.getStatusCode().is2xxSuccessful();
            if (success) {
                log.info("File successfully uploaded to Yandex.Disk: {}", path);
            } else {
                log.warn("File upload status: {}", putResponse.getStatusCode());
            }
            return success;

        } catch (Exception e) {
            log.error("Ошибка при загрузке файла на Яндекс.Диск: {}", e.getMessage(), e);
            return false;
        }
    }

    public String getPublicUrl(String diskPath) {
        try {
            if (!diskPath.startsWith("/")) diskPath = "/" + diskPath;

            publishFile(diskPath);

            Thread.sleep(2000);

            String encodedPath = URLEncoder.encode(diskPath, StandardCharsets.UTF_8);
            String url = BASE_URL + "/resources?path=" + encodedPath + "&fields=public_url,public_key";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "OAuth " + accessToken);
            headers.set("Accept", "application/json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode node = objectMapper.readTree(response.getBody());

                if (node.has("public_url") && !node.get("public_url").isNull()) {
                    String publicUrl = node.get("public_url").asText();
                    if (publicUrl != null && !publicUrl.isEmpty() && !"null".equals(publicUrl)) {
                        log.info("Got public_url: {}", publicUrl);
                        return publicUrl;
                    }
                }

                if (node.has("public_key") && !node.get("public_key").isNull()) {
                    String publicKey = node.get("public_key").asText();
                    if (publicKey != null && !publicKey.isEmpty() && !"null".equals(publicKey)) {
                        String yadiSkUrl = "https://yadi.sk/i/" + publicKey;
                        log.info("Got public_key, building URL: {}", yadiSkUrl);
                        return yadiSkUrl;
                    }
                }

                log.warn("No public_url or public_key in response for: {}", diskPath);
            } else {
                log.warn("Failed to get file info, status: {}", response.getStatusCode());
            }

            return getDownloadUrl(diskPath);

        } catch (Exception e) {
            log.error("Ошибка при получении публичной ссылки для файла {}: {}", diskPath, e.getMessage());
            return null;
        }
    }

    public String getAlternativePublicUrl(String diskPath) {
        try {
            if (!diskPath.startsWith("/")) diskPath = "/" + diskPath;

            String downloadUrl = getDownloadUrl(diskPath);
            if (downloadUrl != null && !downloadUrl.isEmpty()) {
                return downloadUrl;
            }

            String encodedPath = URLEncoder.encode(diskPath, StandardCharsets.UTF_8);
            return "https://disk.yandex.ru/client/disk" + encodedPath;

        } catch (Exception e) {
            log.error("Ошибка при получении альтернативной ссылки: {}", e.getMessage());
            return "https://disk.yandex.ru";
        }
    }

    private String getDownloadUrl(String diskPath) {
        try {
            String encodedPath = URLEncoder.encode(diskPath, StandardCharsets.UTF_8);
            String url = BASE_URL + "/resources/download?path=" + encodedPath;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "OAuth " + accessToken);
            headers.set("Accept", "application/json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode node = objectMapper.readTree(response.getBody());
                if (node.has("href")) {
                    String href = node.get("href").asText();
                    if (href != null && !href.isEmpty()) {
                        log.info("Got download URL: {}", href);
                        return href;
                    }
                }
            }

            return null;

        } catch (Exception e) {
            log.error("Ошибка при получении ссылки для скачивания: {}", e.getMessage());
            return null;
        }
    }

    private void publishFile(String diskPath) {
        try {
            if (!diskPath.startsWith("/")) diskPath = "/" + diskPath;

            String encodedPath = URLEncoder.encode(diskPath, StandardCharsets.UTF_8);
            String url = BASE_URL + "/resources/publish?path=" + encodedPath;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "OAuth " + accessToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.PUT, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("File published successfully: {}", diskPath);
            } else {
                log.warn("Failed to publish file, status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.warn("Ошибка при публикации файла {}: {}", diskPath, e.getMessage());
        }
    }
}