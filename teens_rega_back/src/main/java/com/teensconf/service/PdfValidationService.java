package com.teensconf.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PdfValidationService {

    private static final String[] REQUIRED_PHRASES = {
            "ЦЕРКОВЬ СЛОВО ЖИЗНИ_SBP",
            "МЕСТНАЯ РЕЛИГИОЗНАЯ ОРГАНИЗАЦИЯ ХРИСТИАН ВЕРЫ ЕВАНГЕЛЬСКОЙ (ПЯТИДЕСЯТНИКОВ) ЦЕРКОВЬ \"СЛОВО ЖИЗНИ\" САРАТОВ",
            "6453041398",
            "ПАО СБЕРБАНК"
    };

    private static final String[] BANK_PHRASES = {
            "ПАО СБЕРБАНК",
            "СБЕРБАНК"
    };

    private static final String[] RECIPIENT_PHRASES = {
            "ЦЕРКОВЬ СЛОВО ЖИЗНИ_SBP",
            "МЕСТНАЯ РЕЛИГИОЗНАЯ ОРГАНИЗАЦИЯ ХРИСТИАН ВЕРЫ ЕВАНГЕЛЬСКОЙ (ПЯТИДЕСЯТНИКОВ) ЦЕРКОВЬ \"СЛОВО ЖИЗНИ\" САРАТОВ"
    };

    private static final String REQUIRED_INN = "6453041398";

    public ValidationResult validatePdf(byte[] pdfBytes) {
        try (InputStream inputStream = new ByteArrayInputStream(pdfBytes);
             PDDocument document = PDDocument.load(inputStream)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text == null || text.trim().isEmpty()) {
                return ValidationResult.error("PDF файл пустой или не содержит текста");
            }

            String normalizedText = normalizeText(text);
            List<String> errors = new ArrayList<>();

            boolean hasRecipient = checkRecipient(normalizedText);
            if (!hasRecipient) {
                errors.add("Не найдены реквизиты получателя: ЦЕРКОВЬ_СЛОВО_ЖИЗНИ_SBP или Церковь 'Слово Жизни' Саратов");
            }

            boolean hasInn = checkInn(normalizedText);
            if (!hasInn) {
                errors.add("Не найден ИНН получателя: 6453041398");
            }

            boolean hasBank = checkBank(normalizedText);
            if (!hasBank) {
                errors.add("Не найден банк получателя: ПАО СБЕРБАНК");
            }

            AmountValidationResult amountResult = checkAmount(normalizedText);
            if (!amountResult.isValid()) {
                errors.add(amountResult.getErrorMessage());
            }

            if (!errors.isEmpty()) {
                String errorMessage = String.join("; ", errors);
                log.warn("PDF валидация не пройдена: {}", errorMessage);
                return ValidationResult.error(errorMessage);
            }

            log.info("PDF чек прошел валидацию");
            return ValidationResult.success();

        } catch (Exception e) {
            log.error("Ошибка при чтении PDF файла: {}", e.getMessage());
            return ValidationResult.error("Ошибка при чтении PDF файла: " + e.getMessage());
        }
    }

    private String normalizeText(String text) {
        return text.replace("\u00A0", " ")
                .replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s+", " ")
                .toUpperCase()
                .trim();
    }

    private boolean checkRecipient(String text) {
        for (String phrase : RECIPIENT_PHRASES) {
            if (text.contains(phrase.toUpperCase())) {
                log.debug("Найден получатель: {}", phrase);
                return true;
            }
        }
        return false;
    }

    private boolean checkInn(String text) {
        boolean hasInn = text.contains(REQUIRED_INN);
        if (hasInn) {
            log.debug("Найден ИНН: {}", REQUIRED_INN);
        }
        return hasInn;
    }

    private boolean checkBank(String text) {
        for (String phrase : BANK_PHRASES) {
            if (text.contains(phrase.toUpperCase())) {
                log.debug("Найден банк: {}", phrase);
                return true;
            }
        }
        return false;
    }

    private AmountValidationResult checkAmount(String text) {
        String[] amountPatterns = {
                "500.00", "500,00", "500 РУБ", "500Р", "500 RUR", "500.00",
                "500.00 РУБ", "500,00 РУБ", "500.00Р", "500,00Р", "500"
        };

        for (String pattern : amountPatterns) {
            if (text.contains(pattern)) {
                log.debug("Найдена корректная сумма: {}", pattern);
                return AmountValidationResult.valid();
            }
        }

        if (text.matches(".*[^0-9]500[^0-9].*") ||
                text.matches(".*500[.,]00.*")) {
            return AmountValidationResult.valid();
        }

        List<String> foundAmounts = findAmountsInText(text);
        String foundAmountsStr = foundAmounts.isEmpty() ? "не найдено" : String.join(", ", foundAmounts);

        return AmountValidationResult.invalid("Сумма пожертвования должна быть 500 рублей. Найдены суммы: " + foundAmountsStr);
    }

    private List<String> findAmountsInText(String text) {
        List<String> amounts = new ArrayList<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b(\\d{3,4}([.,]\\d{2})?)\\b");
        java.util.regex.Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String amount = matcher.group(1);
            try {
                double value = Double.parseDouble(amount.replace(",", "."));
                if (value >= 100 && value <= 1000) {
                    amounts.add(amount);
                }
            } catch (NumberFormatException e) {
            }
        }
        return amounts;
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public static class AmountValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private AmountValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static AmountValidationResult valid() {
            return new AmountValidationResult(true, null);
        }

        public static AmountValidationResult invalid(String errorMessage) {
            return new AmountValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}