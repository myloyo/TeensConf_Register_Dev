package com.teensconf.yandex_microservice.kafka;

import com.teensconf.yandex_microservice.dto.RegistrationEventDTO;
import com.teensconf.yandex_microservice.service.YandexExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationEventConsumer {

    private final YandexExportService exportService;

    @KafkaListener(
            topics = "${kafka.topics.registrations:registrations.events}",
            groupId = "${spring.kafka.consumer.group-id:yandex-microservice-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(RegistrationEventDTO event) {
        log.info("Received event: registrationId={}, eventType={}",
                event.getRegistrationId(), event.getEventType());
        exportService.handleEvent(event);
    }
}