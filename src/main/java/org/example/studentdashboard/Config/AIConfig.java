package org.example.studentdashboard.Config;

import org.example.studentdashboard.Advisors.GlobalRewriteAdvisor;
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

    @Bean(name = "openAiChatClient")
    public ChatClient openAiChatClient(OpenAiChatModel chatModel, ChatMemory chatMemory,
                                       VectorStore vectorStore, StudentDashBoardTools studentDashBoardTools){

        MessageChatMemoryAdvisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .order(10)
                .build();
        VectorStoreChatMemoryAdvisor vectorChatMemoryAdvisor = VectorStoreChatMemoryAdvisor.builder(vectorStore)
                .order(15)
                .build();

        VectorStoreDocumentRetriever retriever = VectorStoreDocumentRetriever
                .builder()
                .vectorStore(vectorStore)
                .topK(5)
                .filterExpression(new FilterExpressionBuilder()
                        .in("contentType", "vision_extracted_page", "spreadsheet", "document")
                        .build())
                .build();

        // 4. RAG / File Search (Order 30)
        RetrievalAugmentationAdvisor ragAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever)
                .queryAugmenter(ContextualQueryAugmenter.builder().allowEmptyContext(true).build())
                .order(30)
                .build();

        GlobalRewriteAdvisor globalRewriteAdvisor = new GlobalRewriteAdvisor(chatModel);

        return ChatClient.builder(chatModel)
                .defaultAdvisors(messageChatMemoryAdvisor, vectorChatMemoryAdvisor, globalRewriteAdvisor, ragAdvisor)
                .defaultTools(studentDashBoardTools)
                .build();
    }

    @Bean(name = "anthropicChatClient")
    public ChatClient anthropicChatClient(AnthropicChatModel chatModel, ChatMemory chatMemory,
                                          VectorStore vectorStore, StudentDashBoardTools studentDashBoardTools){

        MessageChatMemoryAdvisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .order(10)
                .build();

        VectorStoreChatMemoryAdvisor vectorChatMemoryAdvisor = VectorStoreChatMemoryAdvisor.builder(vectorStore)
                .order(15)
                .build();

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
                .order(30)
                .build();

        GlobalRewriteAdvisor globalRewriteAdvisor = new GlobalRewriteAdvisor(chatModel);

        return ChatClient.builder(chatModel)
                .defaultAdvisors(messageChatMemoryAdvisor, vectorChatMemoryAdvisor, globalRewriteAdvisor, ragAdvisor)
                .defaultTools(studentDashBoardTools)
                .build();
    }
}