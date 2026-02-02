package com.teensconf.yandex_microservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teensconf.yandex_microservice.dto.ExportRegistrationDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RedisCacheService redisCacheService;

    private List<ExportRegistrationDTO> testRegistrations;

    @BeforeEach
    void setUp() {
        testRegistrations = new ArrayList<>();
        ExportRegistrationDTO dto = new ExportRegistrationDTO();
        dto.setId(1L);
        dto.setFirstName("Test");
        dto.setLastName("User");
        testRegistrations.add(dto);
    }

    @Test
    void cacheAllRegistrations_success() throws JsonProcessingException {
        // Arrange
        String json = "[{\"id\":1,\"firstName\":\"Test\",\"lastName\":\"User\"}]";
        when(objectMapper.writeValueAsString(testRegistrations)).thenReturn(json);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        redisCacheService.cacheAllRegistrations(testRegistrations);

        // Assert
        verify(valueOperations, times(1)).set(eq("yandex:registrations:all"), eq(json), any(Duration.class));
    }

    @Test
    void getAllRegistrationsFromCache_success() throws Exception {
        // Arrange
        String json = "[{\"id\":1,\"firstName\":\"Test\",\"lastName\":\"User\"}]";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("yandex:registrations:all")).thenReturn(json);
        when(objectMapper.readValue(eq(json), any(TypeReference.class)))
                .thenReturn(testRegistrations);

        // Act
        List<ExportRegistrationDTO> result = redisCacheService.getAllRegistrationsFromCache();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void getAllRegistrationsFromCache_nullWhenEmpty() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("yandex:registrations:all")).thenReturn("");

        // Act
        List<ExportRegistrationDTO> result = redisCacheService.getAllRegistrationsFromCache();

        // Assert
        assertNull(result);
    }

    @Test
    void clearCache_success() {
        // Act
        redisCacheService.clearCache();

        // Assert
        verify(redisTemplate, times(1)).delete("yandex:registrations:all");
        verify(redisTemplate, times(1)).delete("yandex:registrations:paid");
        verify(redisTemplate, times(1)).delete("yandex:hash:all");
        verify(redisTemplate, times(1)).delete("yandex:hash:paid");
    }
}