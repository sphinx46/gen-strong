package ru.cs.vsu.social_network.telegram_bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.cs.vsu.social_network.telegram_bot.entity.DocumentEntity;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    /**
     * Проверяет существование документа с таким же именем файла,
     * путем и содержимым, чтобы избежать дублирования
     */
    boolean existsByFileNameAndFilePathAndContentAndChunkIndex(
            String fileName,
            String filePath,
            String content,
            Integer chunkIndex
    );

    /**
     * Находит максимальный индекс чанка для указанного файла
     */
    @Query("SELECT MAX(d.chunkIndex) FROM DocumentEntity d WHERE d.fileName = :fileName")
    Integer findMaxChunkIndexByFileName(@Param("fileName") String fileName);

    /**
     * Подсчитывает количество чанков для указанного файла
     */
    long countByFileName(String fileName);
}