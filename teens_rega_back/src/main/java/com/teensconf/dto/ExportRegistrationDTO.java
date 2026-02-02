package com.teensconf.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExportRegistrationDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String birthDate;
    private String phone;
    private String telegram;
    private String city;
    private Boolean needAccommodation;
    private String church;
    private String role;
    private String parentFullName;
    private String parentPhone;
    private LocalDateTime registrationCreatedAt;
    private LocalDateTime registrationCompletedAt;

    private Long receiptId;
    private Boolean paid;
    private Boolean verified;
    private String fileName;
    private String filePath;
    private String yandexDiskUrl;
}
