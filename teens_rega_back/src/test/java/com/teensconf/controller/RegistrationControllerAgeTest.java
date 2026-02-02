package com.teensconf.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teensconf.dto.RegistrationRequest;
import com.teensconf.entity.Registration;
import com.teensconf.service.RegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerAgeTest {

    private MockMvc mockMvc;

    @Mock
    private RegistrationService registrationService;

    @InjectMocks
    private RegistrationController registrationController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        this.mockMvc = MockMvcBuilders.standaloneSetup(registrationController)
                .setValidator(validator)
                .build();
    }

    private String birthDateForYearsOld(int years) {
        LocalDate d = LocalDate.now().minusYears(years);
        return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private RegistrationRequest baseRequestWithBirthDate(String birthDate) {
        RegistrationRequest r = new RegistrationRequest();
        r.setFirstName("Иван");
        r.setLastName("Иванов");
        r.setEmail("ivan@example.com");
        r.setBirthDate(birthDate);
        r.setPhone("+79161234567");
        r.setTelegram("ivanov");
        r.setCity("Саратов");
        r.setNeedAccommodation(false);
        r.setChurch("Слово");
        r.setRole("подросток");
        r.setWasBefore(false);
        r.setConsentDonation(true);
        r.setConsentPersonalData(true);
        r.setConsentUnder14(true);
        r.setParentFullName("Родитель Иванов");
        r.setParentPhone("+79161112233");
        return r;
    }

    @Test
    void register_age_12_proceedsToPayment__should_return_200() throws Exception {
        Registration reg = new Registration();
        reg.setId(1L);
        when(registrationService.createRegistration(any())).thenReturn(reg);

        RegistrationRequest req = baseRequestWithBirthDate(birthDateForYearsOld(12));

        mockMvc.perform(post("/api/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationId").value(1))
                .andExpect(jsonPath("$.message").value(containsString("Пожалуйста, произведите оплату")));
    }

    @Test
    void register_age_14_proceedsToPayment__should_return_200() throws Exception {
        Registration reg = new Registration();
        reg.setId(2L);
        when(registrationService.createRegistration(any())).thenReturn(reg);

        RegistrationRequest req = baseRequestWithBirthDate(birthDateForYearsOld(14));

        mockMvc.perform(post("/api/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationId").value(2))
                .andExpect(jsonPath("$.message").value(containsString("Пожалуйста, произведите оплату")));
    }

    @Test
    void register_age_16_proceedsToPayment__should_return_200() throws Exception {
        Registration reg = new Registration();
        reg.setId(3L);
        when(registrationService.createRegistration(any())).thenReturn(reg);

        RegistrationRequest req = baseRequestWithBirthDate(birthDateForYearsOld(16));

        mockMvc.perform(post("/api/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationId").value(3))
                .andExpect(jsonPath("$.message").value(containsString("Пожалуйста, произведите оплату")));
    }

    @Test
    void register_age_20_proceedsToPayment__should_return_200() throws Exception {
        Registration reg = new Registration();
        reg.setId(4L);
        when(registrationService.createRegistration(any())).thenReturn(reg);

        RegistrationRequest req = baseRequestWithBirthDate(birthDateForYearsOld(20));

        mockMvc.perform(post("/api/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationId").value(4))
                .andExpect(jsonPath("$.message").value(containsString("Пожалуйста, произведите оплату")));
    }

    @Test
    void shouldReturnBadRequestWhenValidationFails() throws Exception {
        RegistrationRequest invalidRequest = new RegistrationRequest();

        mockMvc.perform(post("/api/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void healthCheck_ShouldReturnHealthy() throws Exception {
        mockMvc.perform(get("/api/registrations/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Service is healthy"));
    }
}
