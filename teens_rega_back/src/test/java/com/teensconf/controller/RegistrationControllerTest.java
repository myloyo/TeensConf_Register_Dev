package com.teensconf.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teensconf.config.TestSecurityConfig;
import com.teensconf.dto.RegistrationRequest;
import com.teensconf.entity.Registration;
import com.teensconf.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegistrationController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegistrationService registrationService;

    @Test
    void shouldRegisterSuccessfully() throws Exception {
        RegistrationRequest request = createValidRegistrationRequest();
        Registration mockRegistration = new Registration();
        mockRegistration.setId(1L);

        when(registrationService.createRegistration(any(RegistrationRequest.class)))
                .thenReturn(mockRegistration);

        mockMvc.perform(post("/api/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationId").value(1))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.amount").doesNotExist()); // Убрали amount из ответа
    }

    @Test
    void shouldReturnBadRequestWhenValidationFails() throws Exception {
        RegistrationRequest invalidRequest = new RegistrationRequest();
        // Пустой запрос - все валидации должны упасть

        mockMvc.perform(post("/api/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenServiceThrowsException() throws Exception {
        RegistrationRequest request = createValidRegistrationRequest();

        when(registrationService.createRegistration(any(RegistrationRequest.class)))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(post("/api/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void healthCheck_ShouldReturnHealthy() throws Exception {
        mockMvc.perform(get("/api/registrations/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Service is healthy"));
    }

    private RegistrationRequest createValidRegistrationRequest() {
        RegistrationRequest request = new RegistrationRequest();
        request.setFirstName("Настя");
        request.setLastName("Тестова");
        request.setEmail("test@example.com");
        request.setBirthDate("01/01/2010");
        request.setPhone("+79990000000");
        request.setTelegram("@testuser");
        request.setCity("Саратов");
        request.setNeedAccommodation(false);
        request.setChurch("Церковь Слово Жизни");
        request.setRole("подросток");
        request.setWasBefore(false);
        request.setConsentUnder14(true);
        request.setConsentDonation(true);
        request.setConsentPersonalData(true);
        request.setParentFullName("Тест Родитель");
        request.setParentPhone("+79991112233");
        return request;
    }
}