package org.example.studentdashboard.Service;
import org.example.studentdashboard.Models.ChatResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatService {

    private final ChatClient openAiClient;

    private final ChatClient anthropicClient;

    private final MemoryJanitorService janitorService;

    private final String systemPrompt = "U r like a tutor,a personallized helper,friend and guide for student,helping the student grow and making him the better version of himself";

    public ChatService(@Qualifier("openAiChatClient") ChatClient openAiClient,
                       @Qualifier("anthropicChatClient") ChatClient anthropicClient,MemoryJanitorService janitorService){
        this.openAiClient = openAiClient;
        this.anthropicClient = anthropicClient;
        this.janitorService = janitorService;
    }

    private ChatResponse useClient(ChatClient client, String msg){
         return client.prompt().user(msg).system(systemPrompt).call()
                 .entity(ChatResponse.class);
    }

    private Flux<String> streamClient(ChatClient client, String msg, String userId){
        janitorService.checkAndSummarize(userId);
        return client.prompt().user(msg).system(systemPrompt).advisors(a -> a.param(ChatMemory.CONVERSATION_ID,userId)).stream().content();
    }

    public ChatResponse chat(String model, String q){
        if(model.equals("claude"))
            return useClient(anthropicClient,q);
        else if(model.equals("chatGpt"))
            return useClient(openAiClient,q);
        throw new RuntimeException("This model isnt integrated");
    }

    public Flux<String> streamChat(String model, String q,String userId){
        if(model.equals("claude"))
            return streamClient(anthropicClient,q,userId);
        else if(model.equals("chatGpt"))
            return streamClient(openAiClient,q,userId);
        throw new RuntimeException("This model isnt integrated");
    }


}
