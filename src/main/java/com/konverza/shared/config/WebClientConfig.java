package com.konverza.shared.config;

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

    @Value("${DEEPSEEK_API_KEY:}")
    private String deepSeekApiKey;

    @Value("${FASTAPI_URL:http://localhost:8000}")
    private String fastApiUrl;

    // No default: Spring fails to start if this is unset (service-to-service auth).
    @Value("${service.core-k-api-key}")
    private String coreKApiKey;

    @Bean("openAiClient")
    public WebClient openAiClient() {
        return WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + openAiApiKey)
                .defaultHeader("Content-Type", "application/json")
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }

    @Bean("deepSeekClient")
    public WebClient deepSeekClient() {
        return WebClient.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .defaultHeader("Authorization", "Bearer " + deepSeekApiKey)
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
                .defaultHeader("X-Service-Key", coreKApiKey)
                .build();
    }
}
