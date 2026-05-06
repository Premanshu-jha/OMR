package org.example.studentdashboard.Controller;
import org.example.studentdashboard.Models.ChatResponse;
import org.example.studentdashboard.Service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping
public class ChatController {

    @Autowired
    ChatService chatService;

    @GetMapping("/{model}/chat")
    public ResponseEntity<ChatResponse> chat(@PathVariable String model, @RequestParam(required = true,value = "q") String q){
        return ResponseEntity.ok(chatService.chat(model,q));
    }

    @GetMapping(value = "/{model}/{userId}/stream-chat",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> streamChat(@PathVariable String model, @RequestParam(required = true,value = "q") String q,@PathVariable String userId){
        return ResponseEntity.ok(chatService.streamChat(model,q,userId));
    }

}
