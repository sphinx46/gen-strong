package ru.cs.vsu.social_network.telegram_bot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_store", indexes = {
        @Index(name = "idx_document_category_active",
                columnList = "category, is_active"),

        @Index(name = "idx_document_file_name",
                columnList = "file_name"),

        @Index(name = "idx_document_created_at",
                columnList = "created_at DESC"),

        @Index(name = "idx_document_last_accessed",
                columnList = "last_accessed_at DESC NULLS LAST"),

        @Index(name = "idx_document_file_chunks",
                columnList = "file_name, chunk_index"),

        @Index(name = "idx_document_language",
                columnList = "language, is_active")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEntity extends BaseEntity {

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    @Column(name = "total_chunks")
    private Integer totalChunks;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "language", length = 10)
    @Builder.Default
    private String language = "ru";

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    public void markAsAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    @Transient
    public String getChunkId() {
        return String.format("%s_%03d", fileName, chunkIndex);
    }
}