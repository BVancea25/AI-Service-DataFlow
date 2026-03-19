package com.dataflow.aiservice.Services;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FinancialAdvisorService {
    private final ChatClient chatClient;

    public FinancialAdvisorService(ChatClient.Builder builder){
        this.chatClient = builder
                .defaultSystem("""
                        You are 'DataFlow AI', a good and fair advisor for users that gives advice based on their data.
                        Instead of hallucinating, you leverage the tools at your disposal to give insights to the users.
                        """)
                .defaultToolNames("budgetStatusFunction")
                .build();
    }

    public String processQuery(String message){
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String tokenValue = "";

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            tokenValue = jwtAuth.getToken().getTokenValue();
        }

        return chatClient.prompt(message)
                .toolContext(Map.of("jwtToken", tokenValue))
                .call()
                .content();
    }
}
