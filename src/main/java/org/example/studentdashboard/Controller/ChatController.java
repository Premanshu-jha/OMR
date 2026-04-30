package org.example.studentdashboard.Controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class ChatController {

    @Autowired
    @Qualifier("openAiChatClient")
    private ChatClient openAiClient;

    @Autowired
    @Qualifier("anthropicChatClient")
    private ChatClient anthropicClient;


    @GetMapping("/{model}/chat")
    public ResponseEntity<String> chat(@PathVariable String model, @RequestParam(required = true,value = "q") String q){
         if(model.equals("claude"))
          return ResponseEntity.ok(anthropicClient.prompt(q).call().content());
         else if(model.equals("chatGpt"))
             return ResponseEntity.ok(openAiClient.prompt(q).call().content());
         throw new RuntimeException("This model isnt integrated");

    }

}
