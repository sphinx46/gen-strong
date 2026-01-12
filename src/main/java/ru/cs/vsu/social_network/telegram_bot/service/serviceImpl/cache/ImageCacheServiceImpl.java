package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl.cache;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.cs.vsu.social_network.telegram_bot.service.cache.ImageCacheService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Value("${training.image.cache.max.size:100}")
    private int cacheMaxSize;

    @Value("${training.image.cache.file.dir:image_cache}")
    private String cacheFileDir;

    private final ConcurrentHashMap<String, CachedImagePath> imagePathCache;
    private ScheduledExecutorService cleanupScheduler;
    private Path cacheDirectory;

    private static class CachedImagePath {
        final String filePath;
        final long creationTime;
        final int width;
        final int height;
        final long fileSize;

        CachedImagePath(String filePath, int width, int height, long fileSize) {
            this.filePath = filePath;
            this.creationTime = System.currentTimeMillis();
            this.width = width;
            this.height = height;
            this.fileSize = fileSize;
        }

        boolean isExpired(long ttlMillis) {
            return System.currentTimeMillis() - creationTime > ttlMillis;
        }
    }

    public ImageCacheServiceImpl() {
        this.imagePathCache = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init() {
        if (cacheEnabled) {
            try {
                cacheDirectory = Paths.get(cacheFileDir);
                if (!Files.exists(cacheDirectory)) {
                    Files.createDirectories(cacheDirectory);
                    log.info("КЕШ_СЕРВИС_ИНИЦИАЛИЗАЦИЯ: директория кеша создана: {}", cacheDirectory.toAbsolutePath());
                }

                cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
                cleanupScheduler.scheduleAtFixedRate(this::cleanupCache,
                        cacheTTLMinutes, cacheTTLMinutes / 2, TimeUnit.MINUTES);
                log.info("КЕШ_СЕРВИС_ИНИЦИАЛИЗАЦИЯ: кеширование путей включено, TTL: {} минут, максимальный размер: {}",
                        cacheTTLMinutes, cacheMaxSize);
            } catch (Exception e) {
                log.error("КЕШ_СЕРВИС_ИНИЦИАЛИЗАЦИЯ_ОШИБКА: не удалось создать директорию кеша: {}", e.getMessage());
                cacheEnabled = false;
            }
        } else {
            log.info("КЕШ_СЕРВИС_ИНИЦИАЛИЗАЦИЯ: кеширование отключено");
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
        imagePathCache.clear();
        log.info("КЕШ_СЕРВИС_ОСТАНОВКА: кеш очищен");
    }

    /** {@inheritDoc} */
    @Override
    public File getImagePathFromCache(String cacheKey) {
        if (!cacheEnabled) {
            return null;
        }

        CachedImagePath cached = imagePathCache.get(cacheKey);
        if (cached != null && !cached.isExpired(TimeUnit.MINUTES.toMillis(cacheTTLMinutes))) {
            File cachedFile = new File(cached.filePath);
            if (cachedFile.exists() && cachedFile.length() > 0) {
                log.info("КЕШ_СЕРВИС_ПОПАДАНИЕ: файл найден в кеше, ключ: {}, размер: {} байт",
                        cacheKey, cached.fileSize);
                return cachedFile;
            } else {
                log.warn("КЕШ_СЕРВИС_ФАЙЛ_НЕ_НАЙДЕН: файл не существует или пустой, удаляем из кеша, ключ: {}", cacheKey);
                imagePathCache.remove(cacheKey);
                return null;
            }
        } else if (cached != null) {
            imagePathCache.remove(cacheKey);
            log.debug("КЕШ_СЕРВИС_УДАЛЕНИЕ: просроченная запись удалена, ключ: {}", cacheKey);
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void cacheImagePath(String cacheKey, File imagePath, int width, int height) {
        if (!cacheEnabled || imagePath == null || !imagePath.exists()) {
            return;
        }

        try {
            long fileSize = imagePath.length();

            String uniqueFileName = generateUniqueCacheFileName(cacheKey);
            Path cachedFilePath = cacheDirectory.resolve(uniqueFileName);

            Files.copy(imagePath.toPath(), cachedFilePath);

            imagePathCache.put(cacheKey, new CachedImagePath(
                    cachedFilePath.toString(),
                    width,
                    height,
                    fileSize
            ));

            log.info("КЕШ_СЕРВИС_СОХРАНЕНИЕ: путь к файлу закэширован, ключ: {}, размер: {} байт, путь: {}",
                    cacheKey, fileSize, cachedFilePath);

            if (imagePathCache.size() > cacheMaxSize) {
                cleanupCache();
            }
        } catch (Exception e) {
            log.warn("КЕШ_СЕРВИС_ОШИБКА_СОХРАНЕНИЯ: ошибка при кэшировании пути к файлу: {}", e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public String generateCycleCacheKey(Double maxBenchPress, String templatePath) {
        String templateName = new File(templatePath).getName();

        if (maxBenchPress != null) {
            double roundedBenchPress = Math.round(maxBenchPress * 10.0) / 10.0;
            return String.format("%s_%.1f", templateName, roundedBenchPress);
        } else {
            return templateName;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void cleanupCache() {
        if (!cacheEnabled) {
            return;
        }

        long ttlMillis = TimeUnit.MINUTES.toMillis(cacheTTLMinutes);
        int initialSize = imagePathCache.size();

        imagePathCache.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isExpired(ttlMillis);
            if (expired) {
                try {
                    Files.deleteIfExists(Paths.get(entry.getValue().filePath));
                } catch (Exception e) {
                    log.warn("КЕШ_СЕРВИС_ОЧИСТКА_ФАЙЛ_ОШИБКА: не удалось удалить файл: {}, ошибка: {}",
                            entry.getValue().filePath, e.getMessage());
                }
            }
            return expired;
        });

        int removedCount = initialSize - imagePathCache.size();
        if (removedCount > 0) {
            log.info("КЕШ_СЕРВИС_ОЧИСТКА: удалено {} просроченных записей", removedCount);
        }

        if (imagePathCache.size() > cacheMaxSize) {
            int toRemove = imagePathCache.size() - cacheMaxSize;
            imagePathCache.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e1.getValue().creationTime, e2.getValue().creationTime))
                    .limit(toRemove)
                    .forEach(entry -> {
                        try {
                            Files.deleteIfExists(Paths.get(entry.getValue().filePath));
                        } catch (Exception e) {
                            log.warn("КЕШ_СЕРВИС_ОЧИСТКА_ФАЙЛ_ОШИБКА: не удалось удалить файл: {}, ошибка: {}",
                                    entry.getValue().filePath, e.getMessage());
                        }
                        imagePathCache.remove(entry.getKey());
                    });
            log.info("КЕШ_СЕРВИС_ОЧИСТКА: удалено {} старых записей для ограничения размера", toRemove);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    /**
     * Генерирует уникальное имя файла для кэша на основе ключа кэша.
     *
     * @param cacheKey ключ кэша
     * @return уникальное имя файла
     */
    private String generateUniqueCacheFileName(String cacheKey) {
        String safeKey = cacheKey.replaceAll("[^a-zA-Z0-9._-]", "_");
        return String.format("cache_%s_%d.png", safeKey, System.currentTimeMillis());
    }
}