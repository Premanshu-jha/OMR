package org.example.studentdashboard.Service;
import org.example.studentdashboard.Models.ChatResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import java.util.List;


@Service
public class ChatService {

    private final ChatClient openAiClient;

    private final ChatClient anthropicClient;

    private final String systemPrompt = "U r a computer science expert!";

    public ChatService(@Qualifier("openAiChatClient") ChatClient openAiClient,
                       @Qualifier("anthropicChatClient") ChatClient anthropicClient){
        this.openAiClient = openAiClient;
        this.anthropicClient = anthropicClient;
    }

    private ChatResponse useClient(ChatClient client, String msg){
         return client.prompt().user(msg).system(systemPrompt).call()
                 .entity(ChatResponse.class);
    }

    public ChatResponse chat(String model, String q){
        if(model.equals("claude"))
            return useClient(anthropicClient,q);
        else if(model.equals("chatGpt"))
            return useClient(openAiClient,q);
        throw new RuntimeException("This model isnt integrated");
    }
}
