package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.cs.vsu.social_network.telegram_bot.bot.GymTelegramBot;
import ru.cs.vsu.social_network.telegram_bot.service.DocumentSenderService;

import java.io.File;

@Slf4j
@Service
public class DocumentSenderServiceImpl implements DocumentSenderService {

    private static final String SERVICE_NAME = "ДОКУМЕНТ_СЕРВИС";

    private final ApplicationContext applicationContext;

    public DocumentSenderServiceImpl(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void sendDocument(final Long telegramId,
                             final File file,
                             final String caption) {
        log.info("{}_ОТПРАВКА_ДОКУМЕНТА_НАЧАЛО: отправка файла пользователю {}, файл: {}",
                SERVICE_NAME, telegramId, file.getName());

        try {
            GymTelegramBot gymTelegramBot = applicationContext.getBean(GymTelegramBot.class);

            if (gymTelegramBot == null) {
                log.error("{}_БОТ_НЕ_НАЙДЕН: не удалось получить бота из контекста", SERVICE_NAME);
                throw new RuntimeException("Бот не инициализирован");
            }

            final SendDocument sendDocument = new SendDocument();
            sendDocument.setChatId(telegramId.toString());
            sendDocument.setDocument(new InputFile(file, file.getName()));

            if (caption != null && !caption.isEmpty()) {
                sendDocument.setCaption(caption);
            }

            gymTelegramBot.execute(sendDocument);

            log.info("{}_ОТПРАВКА_ДОКУМЕНТА_УСПЕХ: файл отправлен пользователю {}, файл: {}",
                    SERVICE_NAME, telegramId, file.getName());

        } catch (TelegramApiException e) {
            log.error("{}_ОТПРАВКА_ДОКУМЕНТА_ОШИБКА: не удалось отправить документ пользователю {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage(), e);
            throw new RuntimeException("Не удалось отправить документ пользователю", e);
        } catch (Exception e) {
            log.error("{}_ОТПРАВКА_ДОКУМЕНТА_ОШИБКА_КОНТЕКСТ: ошибка при отправке файла пользователю {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage(), e);
            throw new RuntimeException("Ошибка отправки документа: " + e.getMessage(), e);
        }
    }
}