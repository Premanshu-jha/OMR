package org.example.studentdashboard.Controller;

import org.example.studentdashboard.Service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class AIController {

    @Autowired
    private AIService aiService;

    @PostMapping("/chat")
    public Mono<String> chatWithClaude(@RequestBody String message){
         return aiService.askClaude(message);
    }

}
