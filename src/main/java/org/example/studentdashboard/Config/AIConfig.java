package org.example.studentdashboard.Config;
import org.example.studentdashboard.Tools.StudentDashBoardTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AIConfig {

    @Bean
    public ChatMemory masterChatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository){
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .maxMessages(20)
                .build();
    }

    @Bean(name = "documentVectorStore")
    public VectorStore documentVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName("document_store") // Your dedicated document table
                .initializeSchema(true)            // Auto-create if it doesn't exist
                .build();
    }

    // ==========================================
    // AGENT 1: OpenAI (The Orchestrator)
    // ==========================================
    @Bean(name = "openAiChatClient")
    public ChatClient openAiChatClient(OpenAiChatModel chatModel,
                                       ChatMemory chatMemory,
                                       VectorStore vectorStore,
                                       StudentDashBoardTools studentDashBoardTools) {


        MessageChatMemoryAdvisor shortTermMemory = MessageChatMemoryAdvisor.builder(chatMemory).order(10).build();
        VectorStoreChatMemoryAdvisor longTermMemory = VectorStoreChatMemoryAdvisor.builder(vectorStore).order(15).build();

        return ChatClient.builder(chatModel)
                .defaultAdvisors(shortTermMemory, longTermMemory)
                .defaultTools(studentDashBoardTools)
                .build();

    }

}