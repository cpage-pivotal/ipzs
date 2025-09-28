package org.tanzu.ipzs.legislation.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAIConfig {

    @Bean
    public ChatClient chatClient(
            ChatClient.Builder chatClientBuilder,
            DateAwareQuestionAnswerAdvisor dateAwareAdvisor) {

        return chatClientBuilder
                .defaultAdvisors(dateAwareAdvisor)
                .build();
    }
}