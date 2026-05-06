package org.example.studentdashboard.Config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class AIConfig {

     @Bean
     public ChatMemory masterChatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository){
        return MessageWindowChatMemory.builder().chatMemoryRepository(jdbcChatMemoryRepository).build();
     }

    @Bean(name = "openAiChatClient")
    public ChatClient openAiChatClient(OpenAiChatModel chatModel, ChatMemory chatMemory){
        MessageChatMemoryAdvisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        return ChatClient.builder(chatModel).defaultAdvisors(messageChatMemoryAdvisor).build();
    }

    @Bean(name = "anthropicChatClient")
    public ChatClient anthropicChatClient(AnthropicChatModel chatModel,ChatMemory chatMemory){
        MessageChatMemoryAdvisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        return ChatClient.builder(chatModel).defaultAdvisors(messageChatMemoryAdvisor).build();
    }
}
