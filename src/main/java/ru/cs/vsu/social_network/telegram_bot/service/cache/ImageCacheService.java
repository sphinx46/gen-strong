package ru.cs.vsu.social_network.telegram_bot.service.cache;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Сервис кэширования изображений.
 */
public interface ImageCacheService {

    /**
     * Получает изображение из кэша.
     *
     * @param cacheKey ключ кэша
     * @return изображение или null, если не найдено
     */
    BufferedImage getImageFromCache(String cacheKey);

    /**
     * Сохраняет изображение в кэш.
     *
     * @param cacheKey ключ кэша
     * @param image изображение для кэширования
     * @param filePath путь к файлу
     * @param width ширина изображения
     * @param height высота изображения
     */
    void cacheImage(String cacheKey, BufferedImage image, String filePath, int width, int height);

    /**
     * Генерирует ключ кэша для файла.
     *
     * @param excelFile Excel файл
     * @param maxBenchPress максимальный вес жима лежа
     * @return ключ кэша
     */
    String generateCacheKey(File excelFile, Double maxBenchPress);

    /**
     * Очищает просроченные записи кэша.
     */
    void cleanupCache();

    /**
     * Проверяет, включено ли кэширование.
     *
     * @return true если кэширование включено
     */
    boolean isCacheEnabled();
}