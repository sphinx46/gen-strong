package ru.cs.vsu.social_network.telegram_bot.service;

import java.io.File;

/**
 * Сервис для отправки документов пользователям через Telegram бота.
 * Отвечает за отправку файлов с тренировочными программами и другими документами.
 */
public interface DocumentSenderService {

    /**
     * Отправляет документ пользователю в Telegram.
     * Используется для отправки Excel файлов с программами тренировок.
     *
     * @param telegramId Telegram ID пользователя
     * @param file файл для отправки
     * @param caption подпись к файлу
     */
    void sendDocument(Long telegramId, File file, String caption);
}