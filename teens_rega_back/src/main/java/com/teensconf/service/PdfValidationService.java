package com.teensconf.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Slf4j
@Service
public class PdfValidationService {

    private static final String[] ALL_KEY_PHRASES = {
            "ЦЕРКОВЬ СЛОВО ЖИЗНИ_SBP",
            "МЕСТНАЯ РЕЛИГИОЗНАЯ ОРГАНИЗНАЦИЯ ХРИСТИАН ВЕРЫ ЕВАНГЕЛЬСКОЙ (ПЯТИДЕСЯТНИКОВ) ЦЕРКОВЬ \"СЛОВО ЖИЗНИ\" САРАТОВ",
            "6453041398",
            "ПАО СБЕРБАНК",
            "СБЕРБАНК",
            "Сбербанк",
            "ЦЕРКОВЬ СЛОВО ЖИЗНИ",
            "СЛОВО ЖИЗНИ САРАТОВ"
    };

    private static final String REFERENCE_SUFFIX = "0011630701";

    public ValidationResult validatePdf(byte[] pdfBytes) {
        try (InputStream inputStream = new ByteArrayInputStream(pdfBytes);
             PDDocument document = PDDocument.load(inputStream)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text == null || text.trim().isEmpty()) {
                return ValidationResult.error("PDF файл пустой или не содержит текста");
            }

            log.debug("Содержимое PDF:\n{}", text);
            String normalizedText = normalizeText(text);
            log.debug("Нормализованный текст:\n{}", normalizedText);

            AmountValidationResult amountResult = checkAmount(normalizedText);
            if (!amountResult.isValid()) {
                return ValidationResult.error(amountResult.getErrorMessage());
            }

            boolean hasAnyKeyPhrase = checkAnyKeyPhrase(normalizedText);
            if (!hasAnyKeyPhrase) {
                log.info("PDF чек не содержит ключевых фраз, но сумма корректна - принимаем");
            } else {
                log.info("PDF чек содержит ключевые фразы и корректную сумму");
            }

            String reference = findReference(normalizedText);
            if (reference != null) {
                if (!reference.endsWith(REFERENCE_SUFFIX)) {
                    log.warn("Найден референс с неправильным окончанием: {}", reference);
                } else {
                    log.info("Найден корректный референс: {}", reference);
                }
            }

            return ValidationResult.success();

        } catch (Exception e) {
            log.error("Ошибка при чтении PDF файла: {}", e.getMessage(), e);
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

    private boolean checkAnyKeyPhrase(String text) {
        for (String phrase : ALL_KEY_PHRASES) {
            if (text.contains(phrase.toUpperCase())) {
                log.debug("Найдена ключевая фраза: {}", phrase);
                return true;
            }
        }
        return false;
    }

    private AmountValidationResult checkAmount(String text) {
        String[] amountPatterns = {
                "500.00", "500,00", "500 РУБ", "500Р", "500 RUR", "500.00",
                "500.00 РУБ", "500,00 РУБ", "500.00Р", "500,00Р", "500",
                "500.0", "500,0", "500.00RUB", "500,00RUB", "500RUB",
                "500,00 Р", "500.00 Р", "500 Р", "500РУБ", "500.00РУБ", "500 RUB", "500 ₽"
        };

        for (String pattern : amountPatterns) {
            if (text.contains(pattern)) {
                log.debug("Найдена корректная сумма по паттерну: {}", pattern);
                return AmountValidationResult.valid();
            }
        }

        // Более гибкая проверка через регулярные выражения
        if (text.matches(".*[^0-9]500[^0-9].*") ||
                text.matches(".*500[.,]00.*") ||
                text.matches(".*500[.,]0[^0-9].*") ||
                text.matches(".*\\b500\\b.*")) {
            log.debug("Найдена корректная сумма по регулярному выражению");
            return AmountValidationResult.valid();
        }

        List<String> foundAmounts = findAmountsInText(text);
        String foundAmountsStr = foundAmounts.isEmpty() ? "не найдено" : String.join(", ", foundAmounts);

        return AmountValidationResult.invalid("Сумма пожертвования должна быть 500 рублей. Найдены суммы: " + foundAmountsStr);
    }

    private List<String> findAmountsInText(String text) {
        List<String> amounts = new ArrayList<>();

        // Исправленные регулярные выражения для поиска сумм
        Pattern[] amountPatterns = {
                Pattern.compile("\\b(\\d{1,4}[.,]\\d{2})\\s*(?:РУБ|RUB|Р|RUR|₽)?"),
                Pattern.compile("(\\d{1,4})\\s*(?:РУБ|RUB|Р|RUR|₽)"),
                Pattern.compile("(\\d{1,4}[.,]\\d{0,2})")
        };

        for (Pattern pattern : amountPatterns) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                try {
                    String amountStr = matcher.group(1).replace(",", ".");
                    double value = Double.parseDouble(amountStr);
                    if (value >= 100 && value <= 1000) {
                        amounts.add(amountStr + " руб");
                    }
                } catch (NumberFormatException | IllegalStateException e) {
                    // Пропускаем некорректные числа
                }
            }
        }
        return amounts;
    }

    private String findReference(String text) {
        Pattern[] referencePatterns = {
                // Простой поиск чисел длиной 10-32 символа
                Pattern.compile("\\b\\d{10,32}\\b"),
                // Поиск с ключевыми словами - исправленные группы
                Pattern.compile("(?:РЕФЕРЕНС|ИДЕНТИФИКАТОР|НОМЕР[\\s]*ОПЕРАЦИИ|СБП)[\\s:]*([A-Z0-9]{10,32})", Pattern.CASE_INSENSITIVE),
                // Поиск любых длинных последовательностей букв и цифр
                Pattern.compile("\\b[A-Z0-9]{10,32}\\b")
        };

        for (Pattern pattern : referencePatterns) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                String reference = null;
                try {
                    // Пытаемся получить группу 1 (для паттернов с группами)
                    if (matcher.groupCount() >= 1) {
                        reference = matcher.group(1);
                    }
                    // Если группа 1 не найдена или null, берем всю найденную строку
                    if (reference == null) {
                        reference = matcher.group(0);
                    }

                    if (reference != null && reference.length() >= 10) {
                        log.debug("Найден потенциальный референс: {}", reference);
                        return reference;
                    }
                } catch (IllegalStateException e) {
                    log.warn("Ошибка при извлечении группы из регулярного выражения: {}", e.getMessage());
                    // Продолжаем поиск с другими паттернами
                }
            }
        }
        return null;
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