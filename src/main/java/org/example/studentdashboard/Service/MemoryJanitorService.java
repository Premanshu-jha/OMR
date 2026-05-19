package org.example.studentdashboard.Service;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemoryJanitorService {

    private ChatClient chatClient;
    private JdbcTemplate jdbcTemplate;

    public MemoryJanitorService(OpenAiChatModel chatModel, JdbcTemplate jdbcTemplate){
         this.chatClient = ChatClient.builder(chatModel).build();
         this.jdbcTemplate = jdbcTemplate;
    }

    @Async
    @Transactional(rollbackFor = Exception.class)
    public void checkAndSummarize(String conversationId){
      String countSql = "SELECT COUNT(*) FROM spring_ai_chat_memory WHERE conversation_id = ?";
      Integer count = jdbcTemplate.queryForObject(countSql,Integer.class,conversationId);
      if(count >= 20) summarizeMessage(conversationId);
    }


    public void summarizeMessage(String conversationId){
        String oldMsgQuery = """
                SELECT content FROM spring_ai_chat_memory 
                WHERE conversation_id = ? 
                ORDER BY timestamp ASC
                LIMIT 20
                """;
        List<String> oldMessages = jdbcTemplate.queryForList(oldMsgQuery,String.class,conversationId);
        String historyToSummarize = String.join("\n",oldMessages);
        String prompt = """
            Summarize the following conversation history strictly and concisely. 
            Focus on the core topics the student is trying to learn.
            
            History:
            %s
            """.formatted(historyToSummarize);

        String summary = this.chatClient.prompt().user(prompt).call().content();

        String archiveSql = """
                INSERT INTO chat_memory_archive (conversation_id, content, type, timestamp)
                SELECT conversation_id, content, type, timestamp 
                FROM spring_ai_chat_memory
                WHERE id IN (
                    SELECT id FROM spring_ai_chat_memory
                    WHERE conversation_id = ?
                    ORDER BY timestamp ASC
                    LIMIT 20
                )
                """;
        jdbcTemplate.update(archiveSql, conversationId);

        String deleteSql = """
                  DELETE FROM spring_ai_chat_memory
                  WHERE id IN (
                  SELECT id FROM spring_ai_chat_memory
                  WHERE conversation_id = ?
                  ORDER BY timestamp ASC
                  LIMIT 20
                  )
                """;

        jdbcTemplate.update(deleteSql,conversationId);

        String insertQuery = """
                 INSERT INTO spring_ai_chat_memory (conversation_id,
                 content,type,timestamp) VALUES (?,?,'SYSTEM',NOW() - INTERVAL '1 day')
                """;

        jdbcTemplate.update(insertQuery,conversationId,summary);
    }


}
