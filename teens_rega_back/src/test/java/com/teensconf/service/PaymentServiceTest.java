package com.teensconf.service;

import com.teensconf.config.TestSecurityConfig;
import com.teensconf.dto.PaymentCompletionRequest;
import com.teensconf.entity.PaymentReceipt;
import com.teensconf.entity.Registration;
import com.teensconf.kafka.EventProducer;
import com.teensconf.repository.PaymentReceiptRepository;
import com.teensconf.repository.RegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Mock
    private PdfValidationService pdfValidationService;

    @Mock
    private EventProducer eventProducer;

    @InjectMocks
    private PaymentService paymentService;

    private Registration registration;
    private final Long REGISTRATION_ID = 1L;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        registration = new Registration();
        registration.setId(REGISTRATION_ID);
        registration.setEmail("test@example.com");
        registration.setFirstName("John");
        registration.setLastName("Doe");

        paymentService.uploadDir = tempDir.toString();
        paymentService.init();
    }

    @Test
    void processPaymentCompletion_WithValidPdfFile_CompletesRegistration() throws IOException {
        PaymentCompletionRequest request = new PaymentCompletionRequest();
        MockMultipartFile pdfFile = new MockMultipartFile(
                "receiptFile",
                "receipt.pdf",
                "application/pdf",
                createValidPdfContent()
        );
        request.setReceiptFile(pdfFile);

        when(registrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
        when(paymentReceiptRepository.save(any(PaymentReceipt.class))).thenAnswer(inv -> inv.getArgument(0));
        PdfValidationService.ValidationResult validationResult =
                PdfValidationService.ValidationResult.success();
        when(pdfValidationService.validatePdf(any())).thenReturn(validationResult);
        PaymentReceipt result = paymentService.processPaymentCompletion(REGISTRATION_ID, request);

        assertNotNull(result);
        assertTrue(result.getVerified());
        assertTrue(result.getPaid());
        assertEquals("receipt.pdf", result.getFileName());
        assertNotNull(result.getFilePath());
        assertTrue(result.getFilePath().contains(".pdf"));
        assertNotNull(registration.getRegistrationCompletedAt());
        verify(emailService, times(1)).sendPaymentSuccessNotification(registration);
        verify(pdfValidationService, times(1)).validatePdf(any());

        assertTrue(Files.exists(Path.of(result.getFilePath())));
    }

    @Test
    void processPaymentCompletion_WithInvalidPdfExtension_ThrowsException() {
        PaymentCompletionRequest request = new PaymentCompletionRequest();
        MockMultipartFile invalidFile = new MockMultipartFile(
                "receiptFile",
                "receipt.txt",
                "text/plain",
                "text content".getBytes()
        );
        request.setReceiptFile(invalidFile);

        when(registrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
        com.teensconf.exception.FileFormatException exception = assertThrows(com.teensconf.exception.FileFormatException.class, () -> {
            paymentService.processPaymentCompletion(REGISTRATION_ID, request);
        });

        assertEquals("Файл должен быть в формате PDF", exception.getMessage());
        verify(paymentReceiptRepository, never()).save(any());
        verify(emailService, never()).sendPaymentSuccessNotification(any());
        verify(pdfValidationService, never()).validatePdf(any());
    }

    @Test
    void processPaymentCompletion_WithPdfFileButInvalidContent_ThrowsException() {
        PaymentCompletionRequest request = new PaymentCompletionRequest();
        MockMultipartFile pdfFile = new MockMultipartFile(
                "receiptFile",
                "receipt.pdf",
                "application/pdf",
                "invalid pdf content".getBytes()
        );
        request.setReceiptFile(pdfFile);

        when(registrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
        PdfValidationService.ValidationResult validationResult =
                PdfValidationService.ValidationResult.error("Чек не прошел валидацию. Не найдены реквизиты получателя");
        when(pdfValidationService.validatePdf(any())).thenReturn(validationResult);
        com.teensconf.exception.FileFormatException exception = assertThrows(com.teensconf.exception.FileFormatException.class, () -> {
            paymentService.processPaymentCompletion(REGISTRATION_ID, request);
        });

        assertTrue(exception.getMessage().contains("Чек не прошел валидацию"));
        verify(paymentReceiptRepository, never()).save(any());
        verify(emailService, never()).sendPaymentSuccessNotification(any());
        verify(pdfValidationService, times(1)).validatePdf(any());
    }

    @Test
    void processPaymentCompletion_WithPdfFileValidationException_ThrowsException() {
        PaymentCompletionRequest request = new PaymentCompletionRequest();
        MockMultipartFile pdfFile = new MockMultipartFile(
                "receiptFile",
                "receipt.pdf",
                "application/pdf",
                createValidPdfContent()
        );
        request.setReceiptFile(pdfFile);

        when(registrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
        when(pdfValidationService.validatePdf(any())).thenThrow(new RuntimeException("Validation error"));
        assertThrows(RuntimeException.class, () -> {
            paymentService.processPaymentCompletion(REGISTRATION_ID, request);
        });

        verify(paymentReceiptRepository, never()).save(any());
        verify(emailService, never()).sendPaymentSuccessNotification(any());
    }

    @Test
    void processPaymentCompletion_NoPaymentData_ThrowsException() {
        PaymentCompletionRequest request = new PaymentCompletionRequest();

        when(registrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
        com.teensconf.exception.ValidationException exception = assertThrows(com.teensconf.exception.ValidationException.class, () -> {
            paymentService.processPaymentCompletion(REGISTRATION_ID, request);
        });

        assertEquals("Не предоставлены данные об оплате", exception.getMessage());
        verify(paymentReceiptRepository, never()).save(any());
        verify(emailService, never()).sendPaymentSuccessNotification(any());
    }

    @Test
    void processPaymentCompletion_WithNullFileName_ThrowsException() {
        PaymentCompletionRequest request = new PaymentCompletionRequest();
        MockMultipartFile fileWithoutName = new MockMultipartFile(
                "receiptFile",
                null,
                "application/pdf",
                createValidPdfContent()
        );
        request.setReceiptFile(fileWithoutName);

        when(registrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));

        assertThrows(com.teensconf.exception.FileFormatException.class, () -> {
            paymentService.processPaymentCompletion(REGISTRATION_ID, request);
        });
    }

    @Test
    void init_CreatesUploadDirectory() {
        PaymentService service = new PaymentService(
                registrationRepository,
                paymentReceiptRepository,
                pdfValidationService,
                emailService,
                eventProducer
        );
        service.uploadDir = tempDir.resolve("new-uploads").toString();

        service.init();

        assertTrue(Files.exists(Path.of(service.uploadDir)));
    }

    @Test
    void processPaymentCompletion_WithPdfFileButInvalidAmount_ThrowsDetailedException() {
        // Given
        PaymentCompletionRequest request = new PaymentCompletionRequest();
        MockMultipartFile pdfFile = new MockMultipartFile(
                "receiptFile",
                "receipt.pdf",
                "application/pdf",
                "pdf with wrong amount".getBytes()
        );
        request.setReceiptFile(pdfFile);

        when(registrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));

        PdfValidationService.ValidationResult validationResult =
                PdfValidationService.ValidationResult.error("Сумма пожертвования должна быть 500 рублей. Найдены суммы: 600, 450");
        when(pdfValidationService.validatePdf(any())).thenReturn(validationResult);

        com.teensconf.exception.FileFormatException exception = assertThrows(com.teensconf.exception.FileFormatException.class, () -> {
            paymentService.processPaymentCompletion(REGISTRATION_ID, request);
        });

        assertTrue(exception.getMessage().contains("Сумма пожертвования должна быть 500 рублей"));
        assertTrue(exception.getMessage().contains("600, 450"));
        verify(paymentReceiptRepository, never()).save(any());
        verify(emailService, never()).sendPaymentSuccessNotification(any());
        verify(pdfValidationService, times(1)).validatePdf(any());
    }

    @Test
    void processPaymentCompletion_WithPdfFileButMissingRecipient_ThrowsDetailedException() {
        // Given
        PaymentCompletionRequest request = new PaymentCompletionRequest();
        MockMultipartFile pdfFile = new MockMultipartFile(
                "receiptFile",
                "receipt.pdf",
                "application/pdf",
                "pdf without recipient".getBytes()
        );
        request.setReceiptFile(pdfFile);

        when(registrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));

        PdfValidationService.ValidationResult validationResult =
                PdfValidationService.ValidationResult.error("Не найдены реквизиты получателя: ЦЕРКОВЬ_СЛОВО_ЖИЗНИ_SBP или Церковь \"Слово Жизни\" Саратов");
        when(pdfValidationService.validatePdf(any())).thenReturn(validationResult);

        com.teensconf.exception.FileFormatException exception = assertThrows(com.teensconf.exception.FileFormatException.class, () -> {
            paymentService.processPaymentCompletion(REGISTRATION_ID, request);
        });

        assertTrue(exception.getMessage().contains("Не найдены реквизиты получателя"));
        assertTrue(exception.getMessage().contains("ЦЕРКОВЬ_СЛОВО_ЖИЗНИ_SBP"));
        verify(paymentReceiptRepository, never()).save(any());
        verify(emailService, never()).sendPaymentSuccessNotification(any());
        verify(pdfValidationService, times(1)).validatePdf(any());
    }

    @Test
    void processPaymentCompletion_WithPdfFileMultipleErrors_ThrowsDetailedException() {
        PaymentCompletionRequest request = new PaymentCompletionRequest();
        MockMultipartFile pdfFile = new MockMultipartFile(
                "receiptFile",
                "receipt.pdf",
                "application/pdf",
                "pdf with multiple errors".getBytes()
        );
        request.setReceiptFile(pdfFile);

        when(registrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));

        PdfValidationService.ValidationResult validationResult =
                PdfValidationService.ValidationResult.error("Не найдены реквизиты получателя: ЦЕРКОВЬ_СЛОВО_ЖИЗНИ_SBP или Церковь \"Слово Жизни\" Саратов; Не найден ИНН получателя: 6453041398; Сумма пожертвования должна быть 500 рублей. Найдены суммы: 300");
        when(pdfValidationService.validatePdf(any())).thenReturn(validationResult);

        com.teensconf.exception.FileFormatException exception = assertThrows(com.teensconf.exception.FileFormatException.class, () -> {
            paymentService.processPaymentCompletion(REGISTRATION_ID, request);
        });

        assertTrue(exception.getMessage().contains("Не найдены реквизиты получателя"));
        assertTrue(exception.getMessage().contains("Не найден ИНН получателя"));
        assertTrue(exception.getMessage().contains("Сумма пожертвования должна быть 500 рублей"));
        verify(paymentReceiptRepository, never()).save(any());
        verify(emailService, never()).sendPaymentSuccessNotification(any());
        verify(pdfValidationService, times(1)).validatePdf(any());
    }

    private byte[] createValidPdfContent() {
        String content = "ЦЕРКОВЬ_СЛОВО_ЖИЗНИ_SBP\n" +
                "ИНН: 6453041398\n" +
                "ПАО СБЕРБАНК\n" +
                "Сумма: 500.00 РУБ\n" +
                "Добровольное пожертвование на конференцию";
        return content.getBytes();
    }
}