package com.teensconf.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teensconf.config.TestSecurityConfig;
import com.teensconf.dto.PaymentCompletionRequest;
import com.teensconf.entity.PaymentReceipt;
import com.teensconf.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    void completeRegistration_WithValidReference_ReturnsSuccess() throws Exception {
        Long registrationId = 1L;
        PaymentReceipt mockReceipt = new PaymentReceipt();
        mockReceipt.setId(1L);
        mockReceipt.setVerified(true);

        when(paymentService.processPaymentCompletion(eq(registrationId), any(PaymentCompletionRequest.class)))
                .thenReturn(mockReceipt);

        MockMultipartFile file = new MockMultipartFile("receiptFile", "", "application/octet-stream", new byte[0]);

        mockMvc.perform(multipart("/api/registrations/{registrationId}/complete", registrationId)
                        .file(file)
                        .param("paymentReference", "A5317171444036040000080011630701")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.receiptId").value(1))
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void completeRegistration_WithInvalidData_ReturnsError() throws Exception {
        Long registrationId = 1L;

        when(paymentService.processPaymentCompletion(eq(registrationId), any(PaymentCompletionRequest.class)))
                .thenThrow(new IllegalArgumentException("Регистрация не найдена"));

        MockMultipartFile file = new MockMultipartFile("receiptFile", "", "application/octet-stream", new byte[0]);

        mockMvc.perform(multipart("/api/registrations/{registrationId}/complete", registrationId)
                        .file(file)
                        .param("paymentReference", "INVALID_REFERENCE")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void completeRegistration_WithUnverifiedReceipt_ReturnsPendingMessage() throws Exception {
        Long registrationId = 1L;
        PaymentReceipt mockReceipt = new PaymentReceipt();
        mockReceipt.setId(1L);
        mockReceipt.setVerified(false); // не верифицирован

        when(paymentService.processPaymentCompletion(eq(registrationId), any(PaymentCompletionRequest.class)))
                .thenReturn(mockReceipt);

        MockMultipartFile file = new MockMultipartFile("receiptFile", "", "application/octet-stream", new byte[0]);

        mockMvc.perform(multipart("/api/registrations/{registrationId}/complete", registrationId)
                        .file(file)
                        .param("paymentReference", "INVALID_REFERENCE")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.verified").value(false))
                .andExpect(jsonPath("$.message").value("Данные об оплате получены. Ожидайте проверки."));
    }

    @Test
    void completeRegistration_WithEmptyRequest_ReturnsError() throws Exception {
        Long registrationId = 1L;

        when(paymentService.processPaymentCompletion(eq(registrationId), any(PaymentCompletionRequest.class)))
                .thenThrow(new IllegalArgumentException("Не предоставлены данные об оплате"));

        mockMvc.perform(multipart("/api/registrations/{registrationId}/complete", registrationId)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}