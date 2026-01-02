package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl.cache;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.cs.vsu.social_network.telegram_bot.service.cache.ImageCacheService;


import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ImageCacheServiceImpl implements ImageCacheService {

    @Value("${training.image.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${training.image.cache.ttl.minutes:60}")
    private long cacheTTLMinutes;

    @Value("${training.image.cache.max.size:50}")
    private int cacheMaxSize;

    private final ConcurrentHashMap<String, CachedImage> imageCache;
    private ScheduledExecutorService cleanupScheduler;

    private static class CachedImage {
        final byte[] compressedImageData;
        final long creationTime;
        final String filePath;
        final int width;
        final int height;
        final int actualRows;
        final int actualColumns;

        CachedImage(byte[] compressedImageData, String filePath, int width, int height, int actualRows, int actualColumns) {
            this.compressedImageData = compressedImageData;
            this.creationTime = System.currentTimeMillis();
            this.filePath = filePath;
            this.width = width;
            this.height = height;
            this.actualRows = actualRows;
            this.actualColumns = actualColumns;
        }

        boolean isExpired(long ttlMillis) {
            return System.currentTimeMillis() - creationTime > ttlMillis;
        }
    }

    public ImageCacheServiceImpl() {
        this.imageCache = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init() {
        if (cacheEnabled) {
            cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
            cleanupScheduler.scheduleAtFixedRate(this::cleanupCache,
                    cacheTTLMinutes, cacheTTLMinutes / 2, TimeUnit.MINUTES);
            log.info("КЕШ_СЕРВИС_ИНИЦИАЛИЗАЦИЯ кеширование включено, TTL: {} минут, максимальный размер: {}",
                    cacheTTLMinutes, cacheMaxSize);
        } else {
            log.info("КЕШ_СЕРВИС_ИНИЦИАЛИЗАЦИЯ кеширование отключено");
        }
    }

    @PreDestroy
    public void destroy() {
        if (cleanupScheduler != null) {
            cleanupScheduler.shutdown();
            try {
                if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        imageCache.clear();
        log.info("КЕШ_СЕРВИС_ОСТАНОВКА кеш очищен");
    }

    @Override
    public BufferedImage getImageFromCache(String cacheKey) {
        if (!cacheEnabled) {
            return null;
        }

        CachedImage cached = imageCache.get(cacheKey);
        if (cached != null && !cached.isExpired(TimeUnit.MINUTES.toMillis(cacheTTLMinutes))) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(cached.compressedImageData);
                BufferedImage image = javax.imageio.ImageIO.read(bais);
                if (image != null) {
                    log.info("КЕШ_СЕРВИС_ПОЛУЧЕНИЕ изображение загружено из кеша ключ {} размер {}x{}",
                            cacheKey, cached.width, cached.height);
                    return image;
                }
            } catch (Exception e) {
                log.warn("КЕШ_СЕРВИС_ОШИБКА_ПОЛУЧЕНИЯ ошибка при загрузке изображения из кеша: {}", e.getMessage());
                imageCache.remove(cacheKey);
            }
        } else if (cached != null) {
            imageCache.remove(cacheKey);
            log.debug("КЕШ_СЕРВИС_УДАЛЕНИЕ просроченная запись удалена ключ {}", cacheKey);
        }

        return null;
    }

    @Override
    public void cacheImage(String cacheKey, BufferedImage image, String filePath, int width, int height) {
        if (!cacheEnabled || image == null) {
            return;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (javax.imageio.ImageIO.write(image, "png", baos)) {
                byte[] compressedData = baos.toByteArray();
                imageCache.put(cacheKey, new CachedImage(compressedData, filePath, width, height, 0, 0));
                log.info("КЕШ_СЕРВИС_СОХРАНЕНИЕ изображение закэшировано ключ {} размер данных {} байт",
                        cacheKey, compressedData.length);

                if (imageCache.size() > cacheMaxSize) {
                    cleanupCache();
                }
            }
        } catch (Exception e) {
            log.warn("КЕШ_СЕРВИС_ОШИБКА_СОХРАНЕНИЯ ошибка при кэшировании изображения: {}", e.getMessage());
        }
    }

    @Override
    public String generateCacheKey(File excelFile, Double maxBenchPress) {
        String fileName = excelFile.getName();
        long fileSize = excelFile.length();
        long lastModified = excelFile.lastModified();

        if (maxBenchPress != null) {
            return String.format("%s_%d_%d_%.1f", fileName, fileSize, lastModified, maxBenchPress);
        } else {
            return String.format("%s_%d_%d", fileName, fileSize, lastModified);
        }
    }

    @Override
    public void cleanupCache() {
        if (!cacheEnabled) {
            return;
        }

        long ttlMillis = TimeUnit.MINUTES.toMillis(cacheTTLMinutes);
        int initialSize = imageCache.size();

        imageCache.entrySet().removeIf(entry -> entry.getValue().isExpired(ttlMillis));

        int removedCount = initialSize - imageCache.size();
        if (removedCount > 0) {
            log.info("КЕШ_СЕРВИС_ОЧИСТКА удалено {} просроченных записей", removedCount);
        }

        if (imageCache.size() > cacheMaxSize) {
            int toRemove = imageCache.size() - cacheMaxSize;
            imageCache.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e1.getValue().creationTime, e2.getValue().creationTime))
                    .limit(toRemove)
                    .forEach(entry -> imageCache.remove(entry.getKey()));
            log.info("КЕШ_СЕРВИС_ОЧИСТКА удалено {} старых записей для ограничения размера", toRemove);
        }

        System.gc();
    }

    @Override
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }
}