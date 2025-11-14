package com.teensconf.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class BirthDateValidator implements ConstraintValidator<ValidBirthDate, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate birthDate = LocalDate.parse(value, formatter);
            LocalDate today = LocalDate.now();

            return !birthDate.isAfter(today) && birthDate.isAfter(today.minusYears(120));

        } catch (DateTimeParseException e) {
            return false;
        }
    }
}