package org.example.studentdashboard.Controller;

import org.example.studentdashboard.Models.ChatResponse;
import org.example.studentdashboard.Models.FileResponse;
import org.example.studentdashboard.Service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    ChatService chatService;

    @GetMapping("/{userId}/block-chat")
    public ResponseEntity<ChatResponse> chat(@RequestParam(required = true, value = "q") String q, @PathVariable String userId){
        return ResponseEntity.ok(chatService.chat(q,userId));
    }

    @PostMapping(value = "/{userId}/stream-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<Map<String, String>>> streamChat(@RequestBody Map<String,String> payload, @PathVariable String userId){
        String query = payload.get("query");
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Chat query cannot be empty");
        }
        return ResponseEntity.ok(chatService.streamChat(query, userId));
    }

    @GetMapping("/{userId}/chat-history")
    public List<Map<String,Object>> getChatHistory(@PathVariable String userId){
        return chatService.getChatHistory(userId);
    }

    @PostMapping("/upload")
    public FileResponse uploadFile(@RequestParam("file") MultipartFile file, @RequestParam(required = false) String rollNumber) throws Exception {
        return chatService.chatUploadFile(file, rollNumber);
    }

    @DeleteMapping("/delete/{fileId}")
    public void deleteFile(@RequestParam String fileName,@RequestParam String fileId){
         chatService.chatDeleteFile(fileName,fileId);
    }

}