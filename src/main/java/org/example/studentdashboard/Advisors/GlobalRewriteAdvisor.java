package org.example.studentdashboard.Advisors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class GlobalRewriteAdvisor implements CallAdvisor, StreamAdvisor {

    ChatClient chatClient;

    public GlobalRewriteAdvisor(ChatModel chatModel){
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    public boolean needRewrite(String msg){
        if(msg.startsWith("[Attached: ")) {
            return false;
        }
        int wordCount = msg.trim().split("\\s+").length;
        if(wordCount <= 4) return true;
        String regex = "\\b(it|this|that|these|those|he|she|they|them|his|her|their|previous|above|other|one|first|last|former|latter|more|else|further|continue|expand|detail|details)\\b";
        Pattern pattern = Pattern.compile(regex,Pattern.CASE_INSENSITIVE);
        return pattern.matcher(msg).find();
    }

    private ChatClientRequest mutateRequest(ChatClientRequest request) {
        String rawUserText = request.prompt().getUserMessage().getText();
        String fixedText = rawUserText;

        if(needRewrite(rawUserText)) {
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

            fixedText = chatClient.prompt().user(rewritePrompt).call().content();

            // 💡 Added this so you can literally watch it work in your console!
            System.out.println("🔄 REWRITE ADVISOR FIRED: [" + rawUserText + "] -> [" + fixedText + "]");
        }

        List<Message> originalMessages = request.prompt().getInstructions();
        List<Message> finalMessages = new ArrayList<>(originalMessages);
        for (int i = originalMessages.size() - 1; i >= 0; i--) {
            Message msg = originalMessages.get(i);
            if (msg.getText().equals(rawUserText)) {
                finalMessages.set(i, new UserMessage(fixedText));
                break;
            }
        }
        return request.mutate().prompt(request.prompt().mutate().messages(finalMessages).build()).build();
    }


    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain callAdvisorChain) {
        return callAdvisorChain.nextCall(mutateRequest(request));
    }


    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain streamAdvisorChain) {
        return streamAdvisorChain.nextStream(mutateRequest(request));
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