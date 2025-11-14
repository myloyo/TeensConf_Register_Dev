package com.teensconf.service;

import com.teensconf.config.TestSecurityConfig;
import com.teensconf.dto.PaymentCompletionRequest;
import com.teensconf.entity.PaymentReceipt;
import com.teensconf.entity.Registration;
import com.teensconf.repository.PaymentReceiptRepository;
import com.teensconf.repository.RegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Import(TestSecurityConfig.class)
class PaymentServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private PaymentReceiptRepository paymentReceiptRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PaymentService paymentService;

    private Registration registration;
    private final Long REGISTRATION_ID = 1L;

    @BeforeEach
    void setUp() {
        registration = new Registration();
        registration.setId(REGISTRATION_ID);
        registration.setEmail("test@example.com");
        paymentService.uploadDir = System.getProperty("java.io.tmpdir");
    }

    @Test
    void processPaymentCompletion_WithValidReference_CompletesRegistration() {
        // Given
        PaymentCompletionRequest request = new PaymentCompletionRequest();
        request.setPaymentReference("A5317171444036040000080011630701");

        when(registrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
        when(paymentReceiptRepository.save(any(PaymentReceipt.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        PaymentReceipt result = paymentService.processPaymentCompletion(REGISTRATION_ID, request);

        // Then
        assertNotNull(result);
        assertTrue(result.getVerified());
        assertTrue(result.getPaid());
        assertEquals(500.0, result.getDonationAmount());
        assertNotNull(registration.getRegistrationCompletedAt());
        verify(emailService, times(1)).sendPaymentSuccessNotification(registration);
    }

    @Test
    void processPaymentCompletion_WithInvalidReference_ReturnsUnverified() {
        // Given
        PaymentCompletionRequest request = new PaymentCompletionRequest();
        request.setPaymentReference("INVALID_REFERENCE");

        when(registrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
        when(paymentReceiptRepository.save(any(PaymentReceipt.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        PaymentReceipt result = paymentService.processPaymentCompletion(REGISTRATION_ID, request);

        // Then
        assertNotNull(result);
        assertFalse(result.getVerified());
        assertFalse(result.getPaid());
        assertNull(registration.getRegistrationCompletedAt());
        verify(emailService, never()).sendPaymentSuccessNotification(any());
    }

    @Test
    void processPaymentCompletion_WithPdfFile_CompletesRegistration() {
        // Given
        PaymentCompletionRequest request = new PaymentCompletionRequest();
        MockMultipartFile pdfFile = new MockMultipartFile(
                "receiptFile",
                "receipt.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );
        request.setReceiptFile(pdfFile);

        when(registrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
        when(paymentReceiptRepository.save(any(PaymentReceipt.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        PaymentReceipt result = paymentService.processPaymentCompletion(REGISTRATION_ID, request);

        // Then
        assertNotNull(result);
        assertTrue(result.getVerified());
        assertTrue(result.getPaid());
        assertEquals("receipt.pdf", result.getFileName());
        assertNotNull(result.getFilePath());
        assertNotNull(registration.getRegistrationCompletedAt());
        verify(emailService, times(1)).sendPaymentSuccessNotification(registration);
    }

    @Test
    void processPaymentCompletion_RegistrationNotFound_ThrowsException() {
        PaymentCompletionRequest request = new PaymentCompletionRequest();
        request.setPaymentReference("A5317171444036040000080011630701");

        when(registrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            paymentService.processPaymentCompletion(REGISTRATION_ID, request);
        });
    }

    @Test
    void processPaymentCompletion_AlreadyCompleted_ThrowsException() {
        // Given
        registration.setRegistrationCompletedAt(java.time.LocalDateTime.now());
        PaymentCompletionRequest request = new PaymentCompletionRequest();
        request.setPaymentReference("A5317171444036040000080011630701");

        when(registrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));

        assertThrows(IllegalArgumentException.class, () -> {
            paymentService.processPaymentCompletion(REGISTRATION_ID, request);
        });
    }

    @Test
    void processPaymentCompletion_NoPaymentData_ThrowsException() {
        PaymentCompletionRequest request = new PaymentCompletionRequest();

        when(registrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));

        assertThrows(IllegalArgumentException.class, () -> {
            paymentService.processPaymentCompletion(REGISTRATION_ID, request);
        });
    }

    @Test
    void processPaymentCompletion_InvalidPdfFile_ReturnsUnverified() {
        // Given
        PaymentCompletionRequest request = new PaymentCompletionRequest();
        MockMultipartFile invalidFile = new MockMultipartFile(
                "receiptFile",
                "receipt.txt",
                "text/plain",
                "text content".getBytes()
        );
        request.setReceiptFile(invalidFile);

        when(registrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
        when(paymentReceiptRepository.save(any(PaymentReceipt.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentReceipt result = paymentService.processPaymentCompletion(REGISTRATION_ID, request);

        // Then
        assertNotNull(result);
        assertFalse(result.getVerified());
        assertFalse(result.getPaid());
        assertNull(registration.getRegistrationCompletedAt());
        verify(emailService, never()).sendPaymentSuccessNotification(any());
    }

    @Test
    void isValidReference_ValidFormat_ReturnsTrue() {
        assertTrue(paymentService.isValidReference("A5317171444036040000080011630701"));
    }

    @Test
    void isValidReference_InvalidFormat_ReturnsFalse() {
        assertFalse(paymentService.isValidReference("INVALID"));
        assertFalse(paymentService.isValidReference("A531717144403604000008001163070"));
        assertFalse(paymentService.isValidReference("A53171714440360400000800116307012"));
        assertFalse(paymentService.isValidReference(null));
    }
}