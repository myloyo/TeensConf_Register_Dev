package com.teensconf.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teensconf.entity.PaymentReceipt;
import com.teensconf.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerFlowTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();
    }

    @Test
    void completePayment_successful() throws Exception {
        PaymentReceipt pr = new PaymentReceipt();
        pr.setId(10L);
        pr.setVerified(true);

        when(paymentService.processPaymentCompletion(any(), any())).thenReturn(pr);
        MockMultipartFile file = new MockMultipartFile("receiptFile", "receipt.pdf", "application/pdf", "dummy".getBytes());

        mockMvc.perform(multipart("/api/registrations/1/complete").file(file)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.receiptId").value(10))
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    void completePayment_invalidDigitalPdf_returnsError() throws Exception {
        doThrow(new com.teensconf.exception.FileFormatException("Загружен пустой или некорректный цифровой PDF"))
                .when(paymentService).processPaymentCompletion(any(), any());

        MockMultipartFile file = new MockMultipartFile("receiptFile", "bad.pdf", "application/pdf", "notpdf".getBytes());

        mockMvc.perform(multipart("/api/registrations/1/complete").file(file)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").isNotEmpty());
    }
}
