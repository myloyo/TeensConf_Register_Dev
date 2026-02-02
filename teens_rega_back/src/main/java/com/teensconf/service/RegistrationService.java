package com.teensconf.service;

import com.teensconf.dto.RegistrationRequest;
import com.teensconf.entity.Registration;
import com.teensconf.repository.RegistrationRepository;
import com.teensconf.dto.events.RegistrationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EmailService emailService;
    private final com.teensconf.kafka.EventProducer eventProducer;

    public Registration createRegistration(@Valid RegistrationRequest request) {
        Registration registration = new Registration();
        registration.setFirstName(request.getFirstName());
        registration.setLastName(request.getLastName());
        registration.setEmail(request.getEmail());
        registration.setBirthDate(request.getBirthDate());
        registration.setPhone(request.getPhone());
        registration.setTelegram(request.getTelegram());
        registration.setCity(request.getCity());
        registration.setNeedAccommodation(request.getNeedAccommodation());
        registration.setChurch(request.getChurch());
        registration.setRole(request.getRole());
        registration.setParentFullName(request.getParentFullName());
        registration.setParentPhone(request.getParentPhone());
        registration.setWasBefore(request.getWasBefore());
        registration.setConsentUnder14(request.getConsentUnder14());
        registration.setConsentDonation(request.getConsentDonation());
        registration.setConsentPersonalData(request.getConsentPersonalData());

        Registration savedRegistration = registrationRepository.save(registration);
        log.info("Регистрация создана с ID: {}", savedRegistration.getId());

        try {
            emailService.sendRegistrationConfirmation(savedRegistration);
        } catch (Exception e) {
            log.error("Ошибка отправки email, но регистрация сохранена: {}", e.getMessage());
        }

        try {
            RegistrationCreatedEvent evt = new RegistrationCreatedEvent(
                    savedRegistration.getId(), savedRegistration.getFirstName(), savedRegistration.getLastName(), savedRegistration.getEmail()
            );
            eventProducer.publish("registrations.events", savedRegistration.getId().toString(), evt);
        } catch (Exception e) {
            log.warn("Не удалось опубликовать событие о регистрации: {}", e.getMessage());
        }

        return savedRegistration;
    }
}