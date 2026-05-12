package org.example.studentdashboard.Advisors;

import org.apache.catalina.User;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class GlobalRewriteAdvisor implements CallAdvisor {

    ChatClient chatClient;

    public GlobalRewriteAdvisor(ChatClient chatClient){
        this.chatClient = chatClient;
    }
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain callAdvisorChain) {
        String rawUserText = request.prompt().getUserMessage().getText();

        String chatHistory = request.prompt().getInstructions().stream()
                .map(msg -> msg.getMessageType().name() + ": " + msg.getText())
                .collect(Collectors.joining("\n"));

        String rewritePrompt = """
            Given the following conversation history, rewrite the user's follow-up 
            question to be a clear, standalone question. Do not answer it, just rewrite it.
            If the question does not need rewriting, just output the original question.
            
            History:
            %s
            
            User Question: %s
            """.formatted(chatHistory, rawUserText);

        String fixedText = chatClient.prompt().user(rewritePrompt).call().content();
        List<Message> originalMessages = request.prompt().getInstructions();
        List<Message> finalMessages = new ArrayList<>(originalMessages);
        for(int i = originalMessages.size()-1;i >=0;i--){
            Message msg = originalMessages.get(i);
          if(msg.getText().equals(rawUserText)) {
              finalMessages.set(i, new UserMessage(fixedText));
              break;
          }
        }
        ChatClientRequest processedRequest = request.mutate().prompt(request.prompt().mutate().messages(finalMessages).build()).build();
        return callAdvisorChain.nextCall(processedRequest);
    }

    @Override
    public String getName() {
        return "GlobalRewriteAdvisor";
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
