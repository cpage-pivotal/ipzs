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
        // Debug logging
        System.out.println("DEBUG: Received chat request with dateContext: " + request.dateContext());

        // Create the base prompt
        var chatClientBuilder = chatClient.prompt(request.message());

        // If date context is provided, we need to pass it through the context
        if (request.dateContext() != null) {
            // Use a custom approach to inject date context into the request
            String response = chatClientBuilder
                    .advisors(spec -> {
                        // Pass the date context as advisor parameters
                        Map<String, Object> contextParams = new HashMap<>();
                        contextParams.put("dateContext", request.dateContext());
                        spec.params(contextParams);
                    })
                    .call()
                    .content();

            return new ChatResponseDto(
                    response,
                    request.sessionId() != null ? request.sessionId() : UUID.randomUUID(),
                    List.of()
            );
        } else {
            // No date context, use regular processing
            String response = chatClientBuilder.call().content();

            return new ChatResponseDto(
                    response,
                    request.sessionId() != null ? request.sessionId() : UUID.randomUUID(),
                    List.of()
            );
        }
    }

    @GetMapping("/health")
    public String health() {
        return "Chat service is running";
    }
}