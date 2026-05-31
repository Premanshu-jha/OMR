package org.example.studentdashboard.Controller;
import org.example.studentdashboard.Models.ChatResponse;
import org.example.studentdashboard.Models.FileResponse;
import org.example.studentdashboard.Service.ChatService;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    ChatService chatService;

    @GetMapping("/{model}/block-chat")
    public ResponseEntity<ChatResponse> chat(@PathVariable String model, @RequestParam(required = true,value = "q") String q){
        return ResponseEntity.ok(chatService.chat(model,q));
    }

    @GetMapping(value = "/{model}/{userId}/stream-chat",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> streamChat(@PathVariable String model, @RequestParam(required = true,value = "q") String q,@PathVariable String userId){
        return ResponseEntity.ok(chatService.streamChat(model,q,userId));
    }

    @GetMapping("/{userId}/chat-history")
    public List<Map<String,Object>> getChatHistory(@PathVariable String userId){
         return chatService.getChatHistory(userId);
    }

    @PostMapping("/upload")
    public void uploadFile(@RequestParam("file") MultipartFile file, @RequestParam(required = false) String rollNumber) throws Exception {
        chatService.chatUploadFile(file,rollNumber);
    }

}
