package com.teensconf.service;

import com.teensconf.config.TestSecurityConfig;
import com.teensconf.dto.RegistrationRequest;
import com.teensconf.entity.Registration;
import com.teensconf.repository.RegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Import(TestSecurityConfig.class)
class RegistrationServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private YandexSheetsService yandexSheetsService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void createRegistration_ValidRequest_ReturnsRegistration() {
        // Given
        RegistrationRequest request = createValidRegistrationRequest();

        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> {
            Registration reg = invocation.getArgument(0);
            reg.setId(1L);
            // Эмулируем вызов @PrePersist
            reg.setRegistrationCreatedAt(LocalDateTime.now());
            return reg;
        });

        // When
        Registration result = registrationService.createRegistration(request);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertNull(result.getRegistrationCompletedAt());
        assertNotNull(result.getRegistrationCreatedAt());

        verify(registrationRepository, times(1)).save(any(Registration.class));
        verify(emailService, times(1)).sendRegistrationConfirmation(any(Registration.class));
        verify(yandexSheetsService, times(1)).uploadRegistrationsToDisk();
    }

    @Test
    void createRegistration_EmailFails_StillSavesRegistration() {
        // Given
        RegistrationRequest request = createValidRegistrationRequest();

        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> {
            Registration reg = invocation.getArgument(0);
            reg.setId(1L);
            reg.setRegistrationCreatedAt(LocalDateTime.now());
            return reg;
        });
        doThrow(new RuntimeException("Email error")).when(emailService).sendRegistrationConfirmation(any());

        // When
        Registration result = registrationService.createRegistration(request);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(registrationRepository, times(1)).save(any(Registration.class));
        verify(yandexSheetsService, times(1)).uploadRegistrationsToDisk();
    }

    @Test
    void createRegistration_SetsAllFieldsCorrectly() {
        // Given
        RegistrationRequest request = createValidRegistrationRequest();

        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> {
            Registration reg = invocation.getArgument(0);
            reg.setId(1L);
            reg.setRegistrationCreatedAt(LocalDateTime.now());
            return reg;
        });

        // When
        Registration result = registrationService.createRegistration(request);

        // Then
        assertNotNull(result);
        assertEquals("Иван", result.getFirstName());
        assertEquals("Иванов", result.getLastName());
        assertEquals("ivan@example.com", result.getEmail());
        assertEquals("15/05/2005", result.getBirthDate());
        assertEquals("+79161234567", result.getPhone());
        assertEquals("ivanov", result.getTelegram());
        assertEquals("Саратов", result.getCity());
        assertFalse(result.getNeedAccommodation());
        assertEquals("Слово Жизни", result.getChurch());
        assertEquals("подросток", result.getRole());
        assertEquals("Родитель Иванов", result.getParentFullName());
        assertEquals("+79161112233", result.getParentPhone());
        assertFalse(result.getWasBefore());
        assertTrue(result.getConsentUnder14());
        assertTrue(result.getConsentDonation());
        assertTrue(result.getConsentPersonalData());
        assertNotNull(result.getRegistrationCreatedAt());
    }

    private RegistrationRequest createValidRegistrationRequest() {
        RegistrationRequest request = new RegistrationRequest();
        request.setFirstName("Иван");
        request.setLastName("Иванов");
        request.setEmail("ivan@example.com");
        request.setBirthDate("15/05/2005");
        request.setPhone("+79161234567");
        request.setTelegram("ivanov");
        request.setCity("Саратов");
        request.setNeedAccommodation(false);
        request.setChurch("Слово Жизни");
        request.setRole("подросток");
        request.setWasBefore(false);
        request.setConsentUnder14(true);
        request.setConsentDonation(true);
        request.setConsentPersonalData(true);
        request.setParentFullName("Родитель Иванов");
        request.setParentPhone("+79161112233");
        return request;
    }
}