package com.teensconf.yandex_microservice.dto;

import lombok.Data;

@Data
public class RegistrationEventDTO {
    private Long registrationId;
    private String eventType;
    private Long timestamp;
}