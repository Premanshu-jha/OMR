package org.example.studentdashboard.Service;
import org.example.studentdashboard.Models.ChatResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private final ChatClient openAiClient;

    private final ChatClient anthropicClient;

    private final MemoryJanitorService janitorService;

    private final JdbcTemplate jdbcTemplate;

    private final String systemPrompt = "U r like a tutor,a personallized helper,friend and guide for student,helping the student grow and making him the better version of himself";

    private final FileService fileService;

    private final UniversalIngestionService universalIngestionService;

    public ChatService(@Qualifier("openAiChatClient") ChatClient openAiClient,
                       @Qualifier("anthropicChatClient") ChatClient anthropicClient,MemoryJanitorService janitorService,JdbcTemplate jdbcTemplate,FileService fileService,UniversalIngestionService universalIngestionService){
        this.openAiClient = openAiClient;
        this.anthropicClient = anthropicClient;
        this.janitorService = janitorService;
        this.jdbcTemplate = jdbcTemplate;
        this.fileService = fileService;
        this.universalIngestionService = universalIngestionService;
    }

    private ChatResponse useClient(ChatClient client, String msg){
         return client.prompt().user(msg).system(systemPrompt).call()
                 .entity(ChatResponse.class);
    }

    private Flux<String> streamClient(ChatClient client, Prompt msg, String userId){
        janitorService.checkAndSummarize(userId);
        return client.prompt(msg).system(systemPrompt).advisors(a -> a.param(ChatMemory.CONVERSATION_ID,userId)).stream().content();
    }

    public ChatResponse chat(String model, String q){
        if(model.equals("claude"))
            return useClient(anthropicClient,q);
        else if(model.equals("chatGpt"))
            return useClient(openAiClient,q);
        throw new RuntimeException("This model isnt integrated");
    }

    public Flux<String> streamChat(String model, String q,String userId){
        Prompt prompt = new Prompt(q);
        if(model.equals("claude"))
            return streamClient(anthropicClient,prompt,userId);
        else if(model.equals("chatGpt"))
            return streamClient(openAiClient,prompt,userId);
        throw new RuntimeException("This model isnt integrated");
    }

    public List<Map<String,Object>> getChatHistory(String userId){
        String sql = """
                SELECT conversation_id,content,type,timestamp FROM spring_ai_chat_memory
                WHERE conversation_id = ?
                
                UNION ALL
                
                SELECT conversation_id,content,type,timestamp FROM chat_memory_archive
                WHERE conversation_id = ?
                
                ORDER BY timestamp ASC
                """;
         return jdbcTemplate.queryForList(sql,userId,userId);
    }

    public void chatUploadFile(MultipartFile file,String rollNumber) throws Exception {
         fileService.uploadFile(file,null,rollNumber);
         universalIngestionService.ingestFile(file);
    }


}
