package ru.cs.vsu.social_network.telegram_bot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = TelegramBotApplication.class
)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.autoconfigure.exclude=",
        "telegram.bot.token=test_token",
        "telegram.bot.username=test_bot",
        "app.security.admin-telegram-ids=123456789"
})
class TelegramBotApplicationTests {

    @Test
    void contextLoads() {
    }
}