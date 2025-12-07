package ru.cs.vsu.social_network.telegram_bot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.profiles.active=integration-test",
                "spring.main.allow-bean-definition-overriding=true",
                "spring.autoconfigure.exclude="
        }
)
@ActiveProfiles("integration-test")
class TelegramBotApplicationTests {

    @Test
    void contextLoads() {
    }
}