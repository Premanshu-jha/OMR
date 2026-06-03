package org.example.studentdashboard.Config;

import org.example.studentdashboard.Tools.StudentDashBoardTools;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    @Bean
    public ChatMemory masterChatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository){
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .maxMessages(20)
                .build();
    }

    // ==========================================
    // AGENT 1: OpenAI (The Orchestrator)
    // ==========================================
    @Bean(name = "openAiChatClient")
    public ChatClient openAiChatClient(OpenAiChatModel chatModel,
                                       ChatMemory chatMemory,
                                       VectorStore vectorStore, // 💥 Uses your single vector_store table!
                                       StudentDashBoardTools studentDashBoardTools) {

        String orchestratorPrompt = """
    You are the Lead Orchestrator AI for the Vidyavriti Student Dashboard.
    
    RULES OF ENGAGEMENT:
    1. GENERAL KNOWLEDGE: If the user asks general questions (math, science, history, coding), answer them directly using your own vast expertise.
    2. CHAT HISTORY: Use your chat memory to remember past conversations and context.
    3. DOCUMENTS & FILES: You DO NOT have direct access to files. 
    4. WHEN TO USE THE TOOL: ONLY trigger the `searchDocumentDatabase` tool if the user explicitly mentions an 'uploaded file', 'document', or asks about their specific syllabus, course rules, or personal academic data that is not in your chat memory.
    """;

        MessageChatMemoryAdvisor shortTermMemory = MessageChatMemoryAdvisor.builder(chatMemory).order(10).build();
        VectorStoreChatMemoryAdvisor longTermMemory = VectorStoreChatMemoryAdvisor.builder(vectorStore).order(15).build();

        return ChatClient.builder(chatModel)
                .defaultSystem(orchestratorPrompt)
                .defaultAdvisors(shortTermMemory, longTermMemory)
                .defaultTools(studentDashBoardTools)
                .build();
    }

    // ==========================================
    // AGENT 2: Anthropic Claude (The RAG Worker)
    // ==========================================
    @Bean(name = "anthropicChatClient")
    public ChatClient anthropicChatClient(AnthropicChatModel chatModel,
                                          VectorStore vectorStore) {

        VectorStoreDocumentRetriever retriever = VectorStoreDocumentRetriever
                .builder()
                .vectorStore(vectorStore)
                .topK(5)
                .filterExpression(new FilterExpressionBuilder()
                        .in("contentType", "vision_extracted_page", "spreadsheet", "document")
                        .build())
                .build();

        RetrievalAugmentationAdvisor ragAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever)
                .queryAugmenter(ContextualQueryAugmenter.builder().allowEmptyContext(true).build())
                .build();

        return ChatClient.builder(chatModel)
                .defaultAdvisors(ragAdvisor) // Claude ONLY does RAG, no memory!
                .build();
    }
}