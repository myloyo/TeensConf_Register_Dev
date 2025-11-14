package com.teensconf.service;

import com.teensconf.entity.Registration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.internet.MimeMessage;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

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
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(registration.getEmail());
            helper.setSubject("Оплата регистрации прошла успешно!");

            String htmlContent = buildPaymentSuccessEmail(registration);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Уведомление об оплате отправлено на: {}", registration.getEmail());

        } catch (Exception e) {
            log.error("Ошибка отправки уведомления об оплате: {}", registration.getEmail(), e);
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