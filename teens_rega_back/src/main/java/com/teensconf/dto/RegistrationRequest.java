package com.teensconf.dto;

import com.teensconf.validation.ValidBirthDate;
import lombok.Data;
import javax.validation.constraints.*;
import java.time.LocalDate;

@Data
public class RegistrationRequest {
    @NotBlank(message = "Имя обязательно")
    @Size(min = 2, max = 50, message = "Имя должно быть от 2 до 50 символов")
    private String firstName;

    @NotBlank(message = "Фамилия обязательна")
    @Size(min = 2, max = 50, message = "Фамилия должна быть от 2 до 50 символов")
    private String lastName;

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    private String email;

    @NotBlank(message = "Дата рождения обязательна")
    @Pattern(regexp = "^\\d{2}/\\d{2}/\\d{4}$", message = "Дата рождения должна быть в формате дд/мм/гггг")
    @ValidBirthDate
    private String birthDate;

    @NotBlank(message = "Телефон обязателен")
    @Pattern(regexp = "^\\+7\\d{10}$", message = "Телефон должен быть в формате +7XXXXXXXXXX")
    private String phone;

    @NotBlank(message = "Telegram обязателен")
    @Size(min = 3, max = 50, message = "Telegram должен быть от 3 до 50 символов")
    private String telegram;

    @NotBlank(message = "Город обязателен")
    @Size(min = 2, max = 50, message = "Город должен быть от 2 до 50 символов")
    private String city;

    private Boolean needAccommodation = false;

    private String church;

    @NotBlank(message = "Роль обязательна")
    @Pattern(regexp = "(?u)^(подросток|служитель)$", message = "Роль должна быть 'подросток' или 'служитель'")
    private String role;

    private String parentFullName;
    private String parentPhone;

    private Boolean wasBefore = false;
    private Boolean consentUnder14 = false;
    private Boolean consentDonation = false;
    private Boolean consentPersonalData = false;

    @AssertTrue(message = "Необходимо согласие для несовершеннолетних")
    public boolean isConsentUnder14Valid() {
        if (isUnder14()) {
            return consentUnder14 != null && consentUnder14;
        }
        return true;
    }

    @AssertTrue(message = "Для несовершеннолетних необходимо указать ФИО родителя")
    public boolean isParentFullNameValid() {
        if (isUnder18()) {
            return parentFullName != null && !parentFullName.trim().isEmpty();
        }
        return true;
    }

    @AssertTrue(message = "Для несовершеннолетних необходимо указать телефон родителя")
    public boolean isParentPhoneValid() {
        if (isUnder18()) {
            return parentPhone != null && parentPhone.matches("^\\+7\\d{10}$");
        }
        return true;
    }

    public boolean isUnder18() {
        if (birthDate == null || !birthDate.matches("^\\d{2}/\\d{2}/\\d{4}$")) {
            return false;
        }
        try {
            String[] parts = birthDate.split("/");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);

            LocalDate birthDate = LocalDate.of(year, month, day);
            return birthDate.plusYears(18).isAfter(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isUnder14() {
        if (birthDate == null || !birthDate.matches("^\\d{2}/\\d{2}/\\d{4}$")) {
            return false;
        }
        try {
            String[] parts = birthDate.split("/");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);

            LocalDate birthDate = LocalDate.of(year, month, day);
            LocalDate fourteenYearsAgo = LocalDate.now().minusYears(14);

            return birthDate.isAfter(fourteenYearsAgo);
        } catch (Exception e) {
            return false;
        }
    }

    @AssertTrue(message = "Необходимо согласие на пожертвование")
    public boolean isConsentDonationValid() {
        return consentDonation != null && consentDonation;
    }

    @AssertTrue(message = "Необходимо согласие на обработку персональных данных")
    public boolean isConsentPersonalDataValid() {
        return consentPersonalData != null && consentPersonalData;
    }
}