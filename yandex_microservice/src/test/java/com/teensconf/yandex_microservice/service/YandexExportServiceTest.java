package com.teensconf.yandex_microservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teensconf.yandex_microservice.dto.ExportRegistrationDTO;
import com.teensconf.yandex_microservice.dto.RegistrationEventDTO;
import com.teensconf.yandex_microservice.yandex.ReceiptUploader;
import com.teensconf.yandex_microservice.yandex.SheetFormatter;
import com.teensconf.yandex_microservice.yandex.YandexClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YandexExportServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private YandexClient yandexClient;

    @Mock
    private SheetFormatter sheetFormatter;

    @Mock
    private ReceiptUploader receiptUploader;

    @Mock
    private RedisCacheService redisCacheService;

    @InjectMocks
    private YandexExportService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "backendBase", "http://backend:8080");
        ReflectionTestUtils.setField(service, "diskFilePath", "/test.xlsx");
        ReflectionTestUtils.setField(service, "receiptsFolder", "/receipts");
        ReflectionTestUtils.setField(service, "uploadReceiptsEnabled", false);
    }

    @Test
    void scheduledExport_uploadsXlsx_whenBackendReturnsLists() throws Exception {
        // Arrange
        List<ExportRegistrationDTO> mockList = new ArrayList<>();
        ExportRegistrationDTO dto = new ExportRegistrationDTO();
        dto.setId(1L);
        mockList.add(dto);

        when(redisCacheService.getHash("yandex:hash:all")).thenReturn(null);
        when(redisCacheService.getHash("yandex:hash:paid")).thenReturn(null);
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("[{\"id\":1}]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .thenReturn(mockList);

        when(sheetFormatter.buildXlsxWithTwoSheets(anyList(), anyList()))
                .thenReturn(new byte[]{1, 2, 3});
        when(yandexClient.uploadFile(any(), any()))
                .thenReturn(true);

        // Act
        service.scheduledExport();

        // Assert
        verify(sheetFormatter, times(1)).buildXlsxWithTwoSheets(anyList(), anyList());
        verify(yandexClient, times(1)).uploadFile(any(), any());
        verify(redisCacheService, times(1)).cacheAllRegistrations(anyList());
        verify(redisCacheService, times(1)).cachePaidRegistrations(anyList());
    }

    @Test
    void handleEvent_evictsCache_and_runsExport() throws Exception {
        // Arrange
        RegistrationEventDTO event = new RegistrationEventDTO();
        event.setRegistrationId(1L);
        event.setEventType("UPDATE");
        event.setTimestamp(System.currentTimeMillis());

        List<ExportRegistrationDTO> mockList = new ArrayList<>();
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("[]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .thenReturn(mockList);
        when(sheetFormatter.buildXlsxWithTwoSheets(anyList(), anyList()))
                .thenReturn(new byte[0]);

        // Act
        service.handleEvent(event);

        // Assert
        verify(redisCacheService, times(1)).clearCache();
        verify(sheetFormatter, times(1)).buildXlsxWithTwoSheets(anyList(), anyList());
    }

    @Test
    void scheduledExport_usesCache_whenNoChanges() throws Exception {
        // Arrange
        List<ExportRegistrationDTO> cachedList = new ArrayList<>();
        String expectedHashForEmptyList = "empty";
        when(redisCacheService.getHash("yandex:hash:all"))
                .thenReturn(expectedHashForEmptyList);
        when(redisCacheService.getHash("yandex:hash:paid"))
                .thenReturn(expectedHashForEmptyList);
        when(redisCacheService.getAllRegistrationsFromCache())
                .thenReturn(cachedList);
        when(redisCacheService.getPaidRegistrationsFromCache())
                .thenReturn(cachedList);

        when(sheetFormatter.buildXlsxWithTwoSheets(anyList(), anyList()))
                .thenReturn(new byte[]{1, 2, 3});
        when(yandexClient.uploadFile(any(), any()))
                .thenReturn(true);

        // Act
        service.scheduledExport();

        // Assert
        verify(sheetFormatter, times(1)).buildXlsxWithTwoSheets(anyList(), anyList());
        verify(yandexClient, times(1)).uploadFile(any(), any());
        verify(redisCacheService, times(1)).getAllRegistrationsFromCache();
        verify(redisCacheService, times(1)).getPaidRegistrationsFromCache();
        verify(redisCacheService, never()).cacheAllRegistrations(anyList());
        verify(redisCacheService, never()).cachePaidRegistrations(anyList());
    }
}