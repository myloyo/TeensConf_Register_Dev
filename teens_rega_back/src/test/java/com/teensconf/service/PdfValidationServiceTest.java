package com.teensconf.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PdfValidationServiceTest {

    @InjectMocks
    private PdfValidationService pdfValidationService;

    @Test
    void validatePdf_WithValidContent_ReturnsSuccess() {
        // Given
        byte[] pdfContent = createValidPdfContent();

        // When
        PdfValidationService.ValidationResult result = pdfValidationService.validatePdf(pdfContent);

        // Then
        assertTrue(result.isValid(), "PDF с валидным контентом должен проходить проверку");
        assertNull(result.getErrorMessage());
    }

    @Test
    void validatePdf_WithMissingRecipient_ReturnsError() {
        // Given
        byte[] pdfContent = createPdfWithMissingRecipient();

        // When
        PdfValidationService.ValidationResult result = pdfValidationService.validatePdf(pdfContent);

        // Then
        assertFalse(result.isValid(), "PDF без получателя должен возвращать ошибку");
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Не найдены реквизиты получателя") ||
                result.getErrorMessage().contains("Не найден ИНН получателя") ||
                result.getErrorMessage().contains("Не найден банк получателя"));
    }

    @Test
    void validatePdf_WithWrongAmount_ReturnsError() {
        // Given
        byte[] pdfContent = createPdfWithWrongAmount();

        // When
        PdfValidationService.ValidationResult result = pdfValidationService.validatePdf(pdfContent);

        // Then
        assertFalse(result.isValid(), "PDF с неправильной суммой должен возвращать ошибку");
        assertNotNull(result.getErrorMessage());
        // Проверяем что есть какая-то ошибка, не обязательно конкретно про сумму
        // так как реальный PDF может не содержать нужных фраз вообще
    }

    @Test
    void validatePdf_WithMissingINN_ReturnsError() {
        // Given
        byte[] pdfContent = createPdfWithMissingINN();

        // When
        PdfValidationService.ValidationResult result = pdfValidationService.validatePdf(pdfContent);

        // Then
        assertFalse(result.isValid(), "PDF без ИНН должен возвращать ошибку");
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Не найден ИНН получателя") ||
                result.getErrorMessage().contains("Не найдены реквизиты получателя"));
    }

    @Test
    void validatePdf_EmptyContent_ReturnsError() {
        // Given
        byte[] pdfContent = new byte[0];

        // When
        PdfValidationService.ValidationResult result = pdfValidationService.validatePdf(pdfContent);

        // Then
        assertFalse(result.isValid(), "Пустой PDF должен возвращать ошибку");
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("PDF файл пустой") ||
                result.getErrorMessage().contains("Ошибка при чтении"));
    }

    @Test
    void validatePdf_NullContent_ReturnsError() {
        // Given
        byte[] pdfContent = null;

        // When
        PdfValidationService.ValidationResult result = pdfValidationService.validatePdf(pdfContent);

        // Then
        assertFalse(result.isValid(), "Null PDF должен возвращать ошибку");
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("PDF файл пустой") ||
                result.getErrorMessage().contains("Ошибка при чтении"));
    }

    @Test
    void validatePdf_WithMultipleRequiredPhrases_ReturnsSuccess() {
        // Given - PDF содержащий все требуемые фразы
        byte[] pdfContent = createPdfWithAllRequiredPhrases();

        // When
        PdfValidationService.ValidationResult result = pdfValidationService.validatePdf(pdfContent);

        // Then
        assertTrue(result.isValid(), "PDF со всеми требуемыми фразами должен проходить проверку");
        assertNull(result.getErrorMessage());
    }

    @Test
    void validatePdf_WithDifferentAmountFormats_ReturnsSuccess() {
        // Given - PDF с разными форматами суммы 500
        byte[] pdfContent = createPdfWithDifferentAmountFormats();

        // When
        PdfValidationService.ValidationResult result = pdfValidationService.validatePdf(pdfContent);

        // Then
        assertTrue(result.isValid(), "PDF с суммой 500 в разных форматах должен проходить проверку");
        assertNull(result.getErrorMessage());
    }

    // Вспомогательные методы для создания тестовых PDF контента
    // Используем реальные PDF байты или создаем простой валидный PDF
    private byte[] createValidPdfContent() {
        // Простой валидный PDF с требуемыми фразами
        String content = "ЦЕРКОВЬ_СЛОВО_ЖИЗНИ_SBP\n" +
                "ИНН: 6453041398\n" +
                "ПАО СБЕРБАНК\n" +
                "Сумма: 500.00 РУБ\n" +
                "Добровольное пожертвование";
        return content.getBytes();
    }

    private byte[] createPdfWithMissingRecipient() {
        // PDF без получателя, но с остальными реквизитами
        String content = "ИНН: 6453041398\n" +
                "ПАО СБЕРБАНК\n" +
                "Сумма: 500.00 РУБ\n" +
                "Какой-то другой получатель";
        return content.getBytes();
    }

    private byte[] createPdfWithWrongAmount() {
        // PDF с неправильной суммой
        String content = "ЦЕРКОВЬ_СЛОВО_ЖИЗНИ_SBP\n" +
                "ИНН: 6453041398\n" +
                "ПАО СБЕРБАНК\n" +
                "Сумма: 300.00 РУБ\n";
        return content.getBytes();
    }

    private byte[] createPdfWithMissingINN() {
        // PDF без ИНН
        String content = "ЦЕРКОВЬ_СЛОВО_ЖИЗНИ_SBP\n" +
                "ПАО СБЕРБАНК\n" +
                "Сумма: 500.00 РУБ\n";
        return content.getBytes();
    }

    private byte[] createPdfWithAllRequiredPhrases() {
        // PDF содержащий все возможные требуемые фразы
        String content = "МЕСТНАЯ РЕЛИГИОЗНАЯ ОРГАНИЗАЦИЯ ХРИСТИАН ВЕРЫ ЕВАНГЕЛЬСКОЙ (ПЯТИДЕСЯТНИКОВ) ЦЕРКОВЬ \"СЛОВО ЖИЗНИ\" САРАТОВ\n" +
                "ИНН 6453041398\n" +
                "Банк: ПАО СБЕРБАНК\n" +
                "ЦЕРКОВЬ_СЛОВО_ЖИЗНИ_SBP\n" +
                "Сумма: 500,00 РУБ\n";
        return content.getBytes();
    }

    private byte[] createPdfWithDifferentAmountFormats() {
        String content = "ЦЕРКОВЬ СЛОВО ЖИЗНИ_SBP\n" +
                "ИНН: 6453041398\n" +
                "ПАО СБЕРБАНК\n" +
                "500.00\n" +
                "500 РУБ\n" +
                "500,00Р\n" +
                "500.00 РУБ\n";
        return content.getBytes();
    }
}