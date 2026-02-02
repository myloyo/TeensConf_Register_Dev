package com.teensconf.yandex_microservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableKafka
@EnableScheduling
@EnableCaching
public class YandexMicroserviceApplication {
    public static void main(String[] args) { SpringApplication.run(YandexMicroserviceApplication.class, args); }
}
