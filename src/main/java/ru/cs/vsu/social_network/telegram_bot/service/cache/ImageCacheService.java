package ru.cs.vsu.social_network.telegram_bot.service.cache;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Сервис кэширования изображений.
 * Оптимизирован для хранения путей к файлам вместо изображений в памяти.
 */
public interface ImageCacheService {

    /**
     * Получает путь к закэшированному файлу изображения.
     *
     * @param cacheKey ключ кэша
     * @return путь к файлу или null, если не найдено
     */
    File getImagePathFromCache(String cacheKey);

    /**
     * Сохраняет путь к файлу изображения в кэш.
     *
     * @param cacheKey ключ кэша
     * @param imagePath путь к файлу изображения
     * @param width ширина изображения
     * @param height высота изображения
     */
    void cacheImagePath(String cacheKey, File imagePath, int width, int height);

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

    /**
     * Альтернативный метод генерации ключа кэша для случая,
     * когда имя шаблона всегда одинаковое.
     *
     * @param maxBenchPress максимальный вес жима лежа
     * @param templatePath путь к шаблону
     * @return ключ кэша
     */
    String generateSimpleCacheKey(Double maxBenchPress, String templatePath);
}