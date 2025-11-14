package com.teensconf;

import com.teensconf.config.TestEmailConfig;
import com.teensconf.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestEmailConfig.class})
class TeensConferenceApplicationTests {

    @Test
    void contextLoads() {
        assertTrue(true);
    }

    @Test
    void basicMathTest() {
        assertEquals(2, 1 + 1);
    }
}