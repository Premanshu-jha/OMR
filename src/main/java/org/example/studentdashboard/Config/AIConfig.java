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
                .defaultSystem("You are a brilliant Student Dashboard Assistant and Tutor. " +
                        "1. FOR GENERAL CONCEPTS: Explain topics clearly, provide examples, and be encouraging. " +
                        "2. FOR PRIVATE DATA: Always check the 'searchDocumentDatabase' or other tools if the user asks " +
                        "about their specific exam papers, marks, or personal documents. " +
                                "DO NOT repeat yourself. DO NOT provide conversational fluff like 'I apologize' or 'I am unable to'. " +
                        "3. BE INDEPENDENT: If a tool call fails or returns no data, DO NOT loop or repeat errors. " +
                        "Instead, gracefully inform the user you couldn't find the specific document, but then offer " +
                        "to explain the concept generally based on your own knowledge.")
                .defaultAdvisors(shortTermMemory,longTermMemory)
                .defaultTools(studentDashBoardTools)
                .build();

    }

}