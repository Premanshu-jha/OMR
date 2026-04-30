package org.example.studentdashboard.Service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


@Service
public class ChatService {

    private final ChatClient openAiClient;

    private final ChatClient anthropicClient;

    private final String systemPrompt = "As an expert in cricket!";

    public ChatService(@Qualifier("openAiChatClient") ChatClient openAiClient,
                       @Qualifier("anthropicChatClient") ChatClient anthropicClient){
        this.openAiClient = openAiClient;
        this.anthropicClient = anthropicClient;
    }

    private String useClient(ChatClient client,String msg){
         return client.prompt().user(msg).system(systemPrompt).call().content();
    }

    public String chat(String model, String q){
        if(model.equals("claude"))
            return useClient(anthropicClient,q);
        else if(model.equals("chatGpt"))
            return useClient(openAiClient,q);
        throw new RuntimeException("This model isnt integrated");
    }
}
