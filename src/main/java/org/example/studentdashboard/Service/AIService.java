package org.example.studentdashboard.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class AIService {

    @Autowired
    ObjectMapper objectMapper;

    public final WebClient client;
    String claudeApiKey;

    public AIService(){
        client = WebClient.create();
        claudeApiKey = System.getenv("CLAUDE_API_KEY");
    }

    public Mono<String> askClaude(String message){
        Map<String,String> reqHeader = new HashMap<>();
        reqHeader.put("Content-Type","application/json");
        reqHeader.put("x-api-key",claudeApiKey);
        reqHeader.put("anthropic-version","2023-06-01");

        Map<String,Object> reqBody = new HashMap<>();
        reqBody.put("model","claude-sonnet-4-20250514");
        reqBody.put("max_tokens",1000);
        reqBody.put("messages", Arrays.asList(Map.of("role","user",
                                                        "content",message)));
         return client.post().uri("https://api.anthropic.com/v1/messages")
                 .headers((set)->{
                     reqHeader.forEach((key,value)->set.add(key,value));
                 }).bodyValue(reqBody)
                 .retrieve()
                 .onStatus(status -> status.isError(),response ->
                      response.bodyToMono(String.class)
                              .flatMap(error -> Mono.error(new RuntimeException("API error: "+error)))
                 ).bodyToMono(String.class)
                 .map(response -> {
                     try{
                         JsonNode root = objectMapper.readTree(response);
                         JsonNode contentArr = root.get("content");
                         StringBuilder str = new StringBuilder();
                         for(JsonNode content:contentArr){
                              str.append(content.get("text"));
                         }
                        return str.toString();
                     } catch (Exception e) {
                         throw new RuntimeException(e);
                     }
                 });

    }

}
