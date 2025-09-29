package org.tanzu.ipzs.legislation.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ChatResponseDto> chat(@RequestBody ChatRequestDto request) {
        try {
            // Validate request
            if (request.message() == null || request.message().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // Debug logging
            System.out.println("DEBUG: Received chat request with dateContext: " + request.dateContext());

            // Create the base prompt
            var chatClientBuilder = chatClient.prompt(request.message());

            String response;
            // If date context is provided, we need to pass it through the context
            if (request.dateContext() != null) {
                // Use a custom approach to inject date context into the request
                response = chatClientBuilder
                        .advisors(spec -> {
                            // Pass the date context as advisor parameters
                            Map<String, Object> contextParams = new HashMap<>();
                            contextParams.put("dateContext", request.dateContext());
                            spec.params(contextParams);
                        })
                        .call()
                        .content();
            } else {
                // No date context, use regular processing
                response = chatClientBuilder.call().content();
            }

            ChatResponseDto chatResponse = new ChatResponseDto(
                    response,
                    request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString(),
                    List.of()
            );

            return ResponseEntity.ok(chatResponse);

        } catch (Exception e) {
            System.err.println("Error processing chat request: " + e.getMessage());
            e.printStackTrace();
            
            ChatResponseDto errorResponse = new ChatResponseDto(
                    "I'm sorry, but I encountered an error processing your request. Please try again.",
                    request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString(),
                    List.of()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/health")
    public String health() {
        return "Chat service is running";
    }
}