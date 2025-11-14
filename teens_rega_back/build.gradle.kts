import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    id("org.springframework.boot") version "2.7.18"
    id("io.spring.dependency-management") version "1.1.0"
}

group = "com.teensconf"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

dependencies {
    // SPRING CORE
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // DB
    implementation("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")

    // DOCS / UTILS
    implementation("org.springdoc:springdoc-openapi-ui:1.6.15")
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
    implementation("me.paulschwarz:spring-dotenv:4.0.0")

    implementation("org.apache.poi:poi:5.2.3")       // для работы с XLS
    implementation("org.apache.poi:poi-ooxml:5.2.3") // для работы с XLSX
    implementation("org.apache.commons:commons-collections4:4.4") // иногда требуется POI

    // LOMBOK
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // TESTS
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.h2database:h2")

    //Рассылка писем подтверждения
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    // QR Code Generation
    implementation("com.google.zxing:core:3.5.1")
    implementation("com.google.zxing:javase:3.5.1")

    //SSL configuration
    implementation("org.apache.httpcomponents:httpclient:4.5.14")

    implementation("org.flywaydb:flyway-core")

    //Сбербанк библиотека
    implementation(files("libs/java-sdk-2.0.3.jar"))

}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

tasks.named("bootJar") {
    enabled = true
}

tasks.named("jar") {
    enabled = false
}
