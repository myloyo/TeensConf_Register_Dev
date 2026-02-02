package com.teensconf.service;

import com.teensconf.entity.Registration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.internet.MimeMessage;
import java.util.Base64;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private QrCodeService qrCodeService;

    @Value("${app.email.from}")
    private String fromEmail;

    private final TemplateEngine templateEngine;

    public EmailService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public void sendRegistrationConfirmation(Registration registration) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(registration.getEmail());
            helper.setSubject("Подтверждение регистрации на конференцию ТИНС");

            String htmlContent = buildRegistrationEmail(registration);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Письмо подтверждения отправлено на: {}", registration.getEmail());

        } catch (Exception e) {
            log.error("Ошибка отправки письма на: {}", registration.getEmail(), e);
        }
    }


    public void sendPaymentSuccessNotification(Registration registration) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(registration.getEmail());
            helper.setSubject("Подтверждение регистрации на конференцию ТИНС");

            byte[] qrCodeBytes = qrCodeService.generateRegistrationQrCodeBytes(registration);

            Context context = new Context();
            context.setVariable("registration", registration);
            context.setVariable("fullName", registration.getFullName());

            String htmlContent = buildPaymentSuccessEmail(registration);
            helper.setText(htmlContent, true);

            if (qrCodeBytes != null) {
                ByteArrayResource qrCodeResource = new ByteArrayResource(qrCodeBytes);
                helper.addInline("qrCode", qrCodeResource, "image/png");
            }

            mailSender.send(mimeMessage);
            log.info("Payment success email sent to: {}", registration.getEmail());

        } catch (Exception e) {
            log.error("Failed to send payment success email to: {}", registration.getEmail(), e);
        }
    }

    private String buildRegistrationEmail(Registration registration) {
        Context context = new Context();
        context.setVariable("registration", registration);
        context.setVariable("fullName", registration.getFirstName() + " " + registration.getLastName());

        return templateEngine.process("register_confirmation", context);
    }

    private String buildPaymentSuccessEmail(Registration registration) {
        Context context = new Context();
        context.setVariable("registration", registration);
        context.setVariable("fullName", registration.getFirstName() + " " + registration.getLastName());

        return templateEngine.process("register_success_payment", context);
    }
}