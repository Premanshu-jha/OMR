package org.example.studentdashboard.Service;

import org.example.studentdashboard.Models.ChatResponse;
import org.example.studentdashboard.Models.FileResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private final ChatClient openAiClient;

    private final MemoryJanitorService janitorService;
    private final JdbcTemplate jdbcTemplate;
    private final FileService fileService;
    private final UniversalIngestionService universalIngestionService;
    private VectorStore documentVectorStore;

    public ChatService(@Qualifier("openAiChatClient") ChatClient openAiClient,
                       MemoryJanitorService janitorService,
                       JdbcTemplate jdbcTemplate,
                       FileService fileService,
                       UniversalIngestionService universalIngestionService,
                       @Qualifier("documentVectorStore") VectorStore documentVectorStore){
        this.openAiClient = openAiClient;
        this.janitorService = janitorService;
        this.jdbcTemplate = jdbcTemplate;
        this.fileService = fileService;
        this.universalIngestionService = universalIngestionService;
        this.documentVectorStore = documentVectorStore;
    }

    public ChatResponse chat(String q,String userId) {
        janitorService.checkAndSummarize(userId);
        return openAiClient.prompt()
                .user(q)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                .call()
                .entity(ChatResponse.class);
    }

    public Flux<Map<String,String>> streamChat(String q, String userId){
        janitorService.checkAndSummarize(userId);

        // Let OpenAI handle the conversation. If it needs documents, it will trigger the Claude tool!
        return openAiClient.prompt()
                .user(q)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                .stream()
                .content()
                .map(chunk -> Map.of("text",chunk))
                .startWith(Map.of("text", ""));
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
        return jdbcTemplate.queryForList(sql, userId, userId);
    }

    public FileResponse chatUploadFile(MultipartFile file, String rollNumber) throws Exception {
        FileResponse fileRes = fileService.uploadFile(file, rollNumber);
        universalIngestionService.ingestFile(file);
        return fileRes;
    }

    public void chatDeleteFile(String fileName,String fileId){
        Filter.Expression filter = new FilterExpressionBuilder().eq("fileName",fileName).build();
        documentVectorStore.delete(filter);
        fileService.deleteFile(fileId);
    }
}