package com.teensconf.yandex_microservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teensconf.yandex_microservice.dto.ExportRegistrationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String ALL_REGISTRATIONS_KEY = "yandex:registrations:all";
    private static final String PAID_REGISTRATIONS_KEY = "yandex:registrations:paid";
    private static final String HASH_ALL_KEY = "yandex:hash:all";
    private static final String HASH_PAID_KEY = "yandex:hash:paid";

    private static final Duration CACHE_TTL = Duration.ofHours(1);


    public void cacheAllRegistrations(List<ExportRegistrationDTO> registrations) {
        try {
            String json = objectMapper.writeValueAsString(registrations);
            redisTemplate.opsForValue().set(ALL_REGISTRATIONS_KEY, json, CACHE_TTL);
            log.debug("All registrations cached in Redis: {} items", registrations.size());
        } catch (Exception e) {
            log.error("Failed to cache all registrations in Redis", e);
        }
    }


    public void cachePaidRegistrations(List<ExportRegistrationDTO> registrations) {
        try {
            String json = objectMapper.writeValueAsString(registrations);
            redisTemplate.opsForValue().set(PAID_REGISTRATIONS_KEY, json, CACHE_TTL);
            log.debug("Paid registrations cached in Redis: {} items", registrations.size());
        } catch (Exception e) {
            log.error("Failed to cache paid registrations in Redis", e);
        }
    }


    public List<ExportRegistrationDTO> getAllRegistrationsFromCache() {
        try {
            String json = redisTemplate.opsForValue().get(ALL_REGISTRATIONS_KEY);
            if (json == null || json.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(json, new TypeReference<List<ExportRegistrationDTO>>() {});
        } catch (Exception e) {
            log.error("Failed to get all registrations from Redis cache", e);
            return null;
        }
    }


    public List<ExportRegistrationDTO> getPaidRegistrationsFromCache() {
        try {
            String json = redisTemplate.opsForValue().get(PAID_REGISTRATIONS_KEY);
            if (json == null || json.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(json, new TypeReference<List<ExportRegistrationDTO>>() {});
        } catch (Exception e) {
            log.error("Failed to get paid registrations from Redis cache", e);
            return null;
        }
    }


    public void saveHash(String key, String hash) {
        try {
            redisTemplate.opsForValue().set(key, hash, CACHE_TTL);
        } catch (Exception e) {
            log.error("Failed to save hash to Redis", e);
        }
    }


    public String getHash(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Failed to get hash from Redis", e);
            return null;
        }
    }

    /**
     * Очистить кэш
     */
    public void clearCache() {
        try {
            redisTemplate.delete(ALL_REGISTRATIONS_KEY);
            redisTemplate.delete(PAID_REGISTRATIONS_KEY);
            redisTemplate.delete(HASH_ALL_KEY);
            redisTemplate.delete(HASH_PAID_KEY);
            log.info("Redis cache cleared");
        } catch (Exception e) {
            log.error("Failed to clear Redis cache", e);
        }
    }


    public void clearAllRegistrationsCache() {
        try {
            redisTemplate.delete(ALL_REGISTRATIONS_KEY);
            redisTemplate.delete(HASH_ALL_KEY);
            log.debug("All registrations cache cleared");
        } catch (Exception e) {
            log.error("Failed to clear all registrations cache", e);
        }
    }


    public void clearPaidRegistrationsCache() {
        try {
            redisTemplate.delete(PAID_REGISTRATIONS_KEY);
            redisTemplate.delete(HASH_PAID_KEY);
            log.debug("Paid registrations cache cleared");
        } catch (Exception e) {
            log.error("Failed to clear paid registrations cache", e);
        }
    }
}