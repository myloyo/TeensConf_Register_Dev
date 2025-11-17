package com.teensconf.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.teensconf.entity.Registration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class QrCodeService {

    public byte[] generateRegistrationQrCodeBytes(Registration registration) {
        try {
            String qrContent = buildQrContent(registration);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 2);

            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 300, 300, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Ошибка генерации QR-кода для регистрации ID: {}", registration.getId(), e);
            return null;
        }
    }

    private String buildQrContent(Registration registration) {
        StringBuilder sb = new StringBuilder();
        sb.append("Подтверждение регистрации ТИНС\n");
        sb.append("ID: ").append(registration.getId()).append("\n");
        sb.append("Имя: ").append(registration.getFirstName()).append("\n");
        sb.append("Фамилия: ").append(registration.getLastName()).append("\n");
        sb.append("Телефон: ").append(registration.getPhone()).append("\n");
        sb.append("Город: ").append(registration.getCity()).append("\n");
        sb.append("Роль: ").append(registration.getRole()).append("\n");
        sb.append("Расселение: ").append(registration.getNeedAccommodation() ? "Да" : "Нет").append("\n");
        if (registration.getPaymentReceipt() != null) {
            sb.append("ID оплаты: ").append(registration.getPaymentReceipt().getId()).append("\n");
        }

        return sb.toString();
    }
}