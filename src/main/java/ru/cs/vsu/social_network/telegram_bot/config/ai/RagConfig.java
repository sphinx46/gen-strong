package ru.cs.vsu.social_network.telegram_bot.config.ai;

import org.springframework.ai.embedding.EmbeddingModel;

import org.springframework.ai.google.genai.GoogleGenAiEmbeddingConnectionDetails;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class RagConfig {

    @Value("${spring.ai.google.genai.embedding.api-key:}")
    private String apiKey;

    @Value("${spring.ai.google.genai.embedding.project-id:}")
    private String projectId;

    @Value("${spring.ai.google.genai.embedding.location:}")
    private String location;

    @Value("${spring.ai.google.genai.embedding.text.options.model:text-embedding-004}")
    private String model;

    @Bean
    public EmbeddingModel embeddingModel() {
        GoogleGenAiEmbeddingConnectionDetails.Builder connectionBuilder =
                GoogleGenAiEmbeddingConnectionDetails.builder();

        if (apiKey != null && !apiKey.isEmpty()) {
            connectionBuilder.apiKey(apiKey);
        } else if (projectId != null && !projectId.isEmpty()) {
            connectionBuilder.projectId(projectId).location(location);
        }

        GoogleGenAiTextEmbeddingOptions options = GoogleGenAiTextEmbeddingOptions.builder()
                .model(model)
                .taskType(GoogleGenAiTextEmbeddingOptions.TaskType.RETRIEVAL_DOCUMENT)
                .build();

        return new GoogleGenAiTextEmbeddingModel(connectionBuilder.build(), options);
    }

    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel).build();
    }
}