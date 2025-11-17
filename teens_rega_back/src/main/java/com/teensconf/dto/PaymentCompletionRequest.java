package com.teensconf.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class PaymentCompletionRequest {
    private MultipartFile receiptFile;
}