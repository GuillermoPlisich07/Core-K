package com.konverza.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${OPENAI_API_KEY:}")
    private String openAiApiKey;

    @Value("${GROQ_API_KEY:}")
    private String groqApiKey;

    @Value("${FASTAPI_URL:http://localhost:8000}")
    private String fastApiUrl;

    @Bean("openAiClient")
    public WebClient openAiClient() {
        return WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + openAiApiKey)
                .defaultHeader("Content-Type", "application/json")
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }

    @Bean("groqClient")
    public WebClient groqClient() {
        return WebClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .defaultHeader("Authorization", "Bearer " + groqApiKey)
                .defaultHeader("Content-Type", "application/json")
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }

    @Bean("fastApiClient")
    public WebClient fastApiClient() {
        return WebClient.builder()
                .baseUrl(fastApiUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
