package org.tanzu.ipzs.legislation.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;
import org.tanzu.ipzs.legislation.model.dto.ChatRequestDto;
import org.tanzu.ipzs.legislation.model.dto.ChatResponseDto;

import java.util.List;
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
        String response = chatClient
                .prompt(request.message())
                .advisors(spec -> {
                    if (request.dateContext() != null) {
                        spec.param("dateContext", request.dateContext());
                    }
                })
                .call()
                .content();

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