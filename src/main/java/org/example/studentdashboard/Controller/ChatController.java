package org.example.studentdashboard.Controller;
import org.example.studentdashboard.Models.ChatResponse;
import org.example.studentdashboard.Service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class ChatController {

    @Autowired
    ChatService chatService;

    @GetMapping("/{model}/chat")
    public ResponseEntity<ChatResponse> chat(@PathVariable String model, @RequestParam(required = true,value = "q") String q){
        return ResponseEntity.ok(chatService.chat(model,q));
    }

}
