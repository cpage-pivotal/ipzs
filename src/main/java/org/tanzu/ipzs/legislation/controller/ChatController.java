package org.tanzu.ipzs.legislation.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;
import org.tanzu.ipzs.legislation.model.dto.ChatRequestDto;
import org.tanzu.ipzs.legislation.model.dto.ChatResponseDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:4200")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping
    public ChatResponseDto chat(@RequestBody ChatRequestDto request) {
        var promptBuilder = chatClient.prompt(request.message());

        if (request.dateContext() != null) {
            Map<String, Object> advisorContext = new HashMap<>();
            advisorContext.put("dateContext", request.dateContext());

            promptBuilder = promptBuilder.advisors(spec -> {
                spec.params(advisorContext);
            });
        }

        String response = promptBuilder.call().content();

        return new ChatResponseDto(
                response,
                request.sessionId() != null ? request.sessionId() : UUID.randomUUID(),
                List.of()
        );
    }

    @GetMapping("/health")
    public String health() {
        return "Chat service is running";
    }
}