package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;
import ru.cs.vsu.social_network.telegram_bot.entity.DocumentEntity;
import ru.cs.vsu.social_network.telegram_bot.repository.DocumentRepository;
import ru.cs.vsu.social_network.telegram_bot.service.DocumentService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final VectorStore vectorStore;
    private final ResourcePatternResolver resourcePatternResolver;

    private static final String CATEGORY_BIOCHEMISTRY = "biochemistry";
    private static final String CATEGORY_NUTRITION = "nutrition";
    private static final String CATEGORY_TRAINING = "training";
    private static final String CATEGORY_GENERAL = "general";

    private static final String CONTENT_TYPE_TEXT = "text/plain";
    private static final String CONTENT_TYPE_MARKDOWN = "text/markdown";
    private static final String CONTENT_TYPE_PDF = "application/pdf";
    private static final String CONTENT_TYPE_UNKNOWN = "unknown";

    private static final String LANGUAGE_RU = "ru";
    private static final String LANGUAGE_EN = "en";

    private static final String SOURCE_TELEGRAM_BOT = "telegram_bot";

    @Value("${app.chunk-size:1000}")
    private int chunkSize;

    @Value("${app.documentPath:classpath*:documents/*.pdf}")
    private String documentPath;

    @Override
    @Transactional
    public void loadDocuments() throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Resource[] resources = resourcePatternResolver.getResources(documentPath);
        log.info("Найдено документов для обработки: {}", resources.length);

        if (resources.length == 0) {
            log.warn("Документы для обработки не найдены по пути: {}", documentPath);
            return;
        }

        TokenTextSplitter textSplitter = TokenTextSplitter.builder()
                .withChunkSize(chunkSize)
                .build();

        int processedCount = 0;
        int skippedCount = 0;
        int totalChunks = 0;

        for (Resource document : resources) {
            String fileName = document.getFilename();
            if (fileName == null) {
                continue;
            }

            try {
                if (isDocumentAlreadyProcessed(fileName)) {
                    log.debug("Документ уже обработан: {}", fileName);
                    skippedCount++;
                    continue;
                }

                String content = document.getContentAsString(StandardCharsets.UTF_8);
                if (content == null || content.trim().isEmpty()) {
                    continue;
                }

                String filePath = document.getURI().toString();
                String contentType = detectContentType(fileName);
                String category = detectCategory(filePath);
                String language = detectLanguage(content);

                Document springAiDocument = new Document(content, createMetadata(fileName, filePath, contentType, language));
                List<Document> chunks = textSplitter.split(List.of(springAiDocument));

                List<DocumentEntity> entities = new ArrayList<>(chunks.size());
                List<Document> vectorDocuments = new ArrayList<>(chunks.size());

                for (int i = 0; i < chunks.size(); i++) {
                    Document chunk = chunks.get(i);
                    String chunkContent = chunk.getFormattedContent();

                    if (documentRepository.existsByFileNameAndFilePathAndContentAndChunkIndex(
                            fileName, filePath, chunkContent, i)) {
                        continue;
                    }

                    DocumentEntity entity = DocumentEntity.builder()
                            .fileName(fileName)
                            .filePath(filePath)
                            .contentType(contentType)
                            .content(chunkContent)
                            .chunkIndex(i)
                            .totalChunks(chunks.size())
                            .category(category)
                            .language(language)
                            .tokenCount(estimateTokenCount(chunkContent))
                            .isActive(true)
                            .lastAccessedAt(LocalDateTime.now())
                            .build();

                    entities.add(entity);

                    Document vectorDocument = new Document(chunkContent, createVectorStoreMetadata(entity));
                    vectorDocuments.add(vectorDocument);
                }

                if (!entities.isEmpty()) {
                    vectorStore.add(vectorDocuments);
                    documentRepository.saveAll(entities);

                    processedCount++;
                    totalChunks += entities.size();
                    log.info("Обработан документ: {} (чанков: {})", fileName, entities.size());
                }

            } catch (Exception e) {
                log.error("Ошибка обработки документа: {}", fileName, e);
            }
        }

        stopWatch.stop();
        log.info("Загрузка завершена. Обработано: {}, Пропущено: {}, Чанков: {}, Время: {} мс",
                processedCount, skippedCount, totalChunks, stopWatch.getTotalTimeMillis());
    }

    private String detectContentType(String fileName) {
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".txt")) return CONTENT_TYPE_TEXT;
        if (lowerFileName.endsWith(".md")) return CONTENT_TYPE_MARKDOWN;
        if (lowerFileName.endsWith(".pdf")) return CONTENT_TYPE_PDF;
        return CONTENT_TYPE_UNKNOWN;
    }

    private String detectCategory(String filePath) {
        if (filePath.contains("/biochemistry/")) return CATEGORY_BIOCHEMISTRY;
        if (filePath.contains("/nutrition/")) return CATEGORY_NUTRITION;
        if (filePath.contains("/training/")) return CATEGORY_TRAINING;
        return CATEGORY_GENERAL;
    }

    private String detectLanguage(String content) {
        long cyrillicCount = content.chars().filter(ch ->
                Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.CYRILLIC).count();
        long latinCount = content.chars().filter(ch ->
                Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.BASIC_LATIN).count();

        return cyrillicCount > latinCount ? LANGUAGE_RU : LANGUAGE_EN;
    }

    private int estimateTokenCount(String text) {
        return text.split("\\s+").length;
    }

    private Map<String, Object> createMetadata(String fileName, String filePath, String contentType, String language) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileName", fileName);
        metadata.put("filePath", filePath);
        metadata.put("contentType", contentType);
        metadata.put("source", SOURCE_TELEGRAM_BOT);
        metadata.put("processed_at", LocalDateTime.now().toString());
        metadata.put("language", language);
        return metadata;
    }

    private Map<String, Object> createVectorStoreMetadata(DocumentEntity entity) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", String.format("%s_%03d", entity.getFileName(), entity.getChunkIndex()));
        metadata.put("fileName", entity.getFileName());
        metadata.put("chunkIndex", entity.getChunkIndex());
        metadata.put("category", entity.getCategory());
        metadata.put("language", entity.getLanguage());
        metadata.put("filePath", entity.getFilePath());
        metadata.put("contentType", entity.getContentType());
        return metadata;
    }

    private boolean isDocumentAlreadyProcessed(String fileName) {
        return documentRepository.countByFileName(fileName) > 0;
    }
}