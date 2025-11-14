package com.teensconf;

import com.teensconf.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@Slf4j
public class TeensConferenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeensConferenceApplication.class, args);
    }

    @Bean
    @Profile("!test")
    public CommandLineRunner initPaymentService(PaymentService paymentService) {
        return args -> {
            paymentService.init();
            log.info("Payment service initialized");
        };
    }
}