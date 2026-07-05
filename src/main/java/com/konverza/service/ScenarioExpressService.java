package com.konverza.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.konverza.dto.ScenarioExpressRequest;
import com.konverza.entity.Scenario;
import com.konverza.repository.ScenarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ScenarioExpressService {

    private final WebClient groqClient;
    private final ScenarioRepository scenarioRepository;
    private final ObjectMapper objectMapper;

    public ScenarioExpressService(
            @Qualifier("groqClient") WebClient groqClient,
            ScenarioRepository scenarioRepository,
            ObjectMapper objectMapper) {
        this.groqClient = groqClient;
        this.scenarioRepository = scenarioRepository;
        this.objectMapper = objectMapper;
    }

    public Scenario generateAndSave(ScenarioExpressRequest req) {
        String prompt = buildExpressGenerationPrompt(req);
        String rawJson = callGroq(prompt);
        Map<?, ?> parsed = parseWithRetry(prompt, rawJson);

        Scenario scenario = Scenario.builder()
                .name(req.getName())
                .industry(req.getIndustry())
                .clientPersona(req.getClientPersona())
                .difficulty(req.getDifficulty())
                .maxDurationMinutes(30)
                .createdBy("EXPRESS_AI")
                .systemPrompt(asString(parsed, "system_prompt"))
                .productContext(asString(parsed, "product_context"))
                .objectionsGuide(toJsonString(parsed.get("objections_guide")))
                .faq(toJsonString(parsed.get("faq")))
                .paymentInfo(asString(parsed, "payment_info"))
                .forbiddenPhrases(toJsonString(parsed.get("forbidden_phrases")))
                .avatarVoiceId(asString(parsed, "avatar_voice_id"))
                .build();

        return scenarioRepository.save(scenario);
    }

    public String regenerateSection(UUID scenarioId, String section) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new RuntimeException("Escenario no encontrado: " + scenarioId));

        String prompt = buildRegenerateSectionPrompt(scenario, section);
        String content = callGroq(prompt);
        String sanitized = sanitizeJson(content);

        switch (section) {
            case "system_prompt"     -> scenario.setSystemPrompt(sanitized);
            case "objections_guide"  -> scenario.setObjectionsGuide(sanitized);
            case "faq"               -> scenario.setFaq(sanitized);
            case "forbidden_phrases" -> scenario.setForbiddenPhrases(sanitized);
            default -> throw new IllegalArgumentException("Sección desconocida: " + section);
        }
        scenarioRepository.save(scenario);
        return sanitized;
    }

    private String buildExpressGenerationPrompt(ScenarioExpressRequest req) {
        return """
                Sos un experto en entrenamiento de vendedores para el mercado uruguayo y argentino.
                Generá el contenido de un escenario de roleplay para entrenamiento de ventas.

                Parámetros del escenario:
                - Producto/servicio: %s
                - Descripción: %s
                - Precio: %s
                - Diferencial clave: %s
                - Tipo de cliente: %s (ANGRY=muy enojado, DIFFICULT=difícil, INDIFFERENT=indiferente, DEMANDING=muy exigente)
                - Dificultad: %s

                Generá un JSON con exactamente esta estructura (sin markdown, sin backticks):
                {
                  "system_prompt": "Instrucción completa para el avatar que juega el rol del cliente. En español rioplatense (voseo). Debe incluir: personalidad, historia de fondo, motivaciones, cómo reacciona a diferentes situaciones. Mínimo 200 palabras.",
                  "product_context": "Contexto del producto que el vendedor puede usar. Info técnica, beneficios, casos de uso. Mínimo 150 palabras.",
                  "objections_guide": [
                    {
                      "trigger": "frase o situación que dispara la objeción",
                      "objection": "texto exacto de la objeción del cliente",
                      "hint": "pista para el vendedor sobre cómo manejarla"
                    }
                  ],
                  "faq": [
                    { "question": "pregunta frecuente", "answer": "respuesta esperada del vendedor" }
                  ],
                  "payment_info": "Información sobre formas de pago, financiación, descuentos disponibles.",
                  "forbidden_phrases": ["frase que el vendedor nunca debe decir", "otra frase negativa"],
                  "avatar_voice_id": "TAVUS_PERSONA_ID_PLACEHOLDER"
                }

                El system_prompt debe hacer que el cliente sea creíble y consistente con el tipo '%s'.
                Las objeciones deben ser 5, realistas y específicas al producto indicado.
                El FAQ debe tener 8 items.
                Los forbidden_phrases deben ser 6 frases típicas de vendedores inexpertos.
                Todo en español rioplatense (Uruguay/Argentina).
                Responde SOLO el JSON, sin texto adicional, sin markdown.
                """.formatted(
                req.getProductName(), req.getProductDescription(),
                req.getPriceRange(), req.getKeyDifferentiator(),
                req.getClientPersona(), req.getDifficulty(), req.getClientPersona()
        );
    }

    private String buildRegenerateSectionPrompt(Scenario scenario, String section) {
        return switch (section) {
            case "system_prompt" -> """
                    Regenerá el system_prompt del siguiente escenario de ventas.
                    Tipo de cliente: %s. Dificultad: %s.
                    Contexto del producto: %s
                    Devolvé SOLO el texto del system_prompt (sin JSON, sin markdown).
                    En español rioplatense (voseo). Mínimo 200 palabras.
                    """.formatted(scenario.getClientPersona(), scenario.getDifficulty(), scenario.getProductContext());
            case "objections_guide" -> """
                    Generá 5 objeciones realistas para el siguiente escenario de ventas.
                    Tipo de cliente: %s. Producto/contexto: %s
                    Devolvé SOLO un JSON array (sin markdown):
                    [{"trigger":"...","objection":"...","hint":"..."}]
                    """.formatted(scenario.getClientPersona(), scenario.getProductContext());
            case "faq" -> """
                    Generá 8 preguntas frecuentes para el siguiente escenario de ventas.
                    Producto/contexto: %s
                    Devolvé SOLO un JSON array (sin markdown):
                    [{"question":"...","answer":"..."}]
                    """.formatted(scenario.getProductContext());
            case "forbidden_phrases" -> """
                    Generá 6 frases que un vendedor inexperto NO debe decir al vender este producto.
                    Producto/contexto: %s. Tipo de cliente: %s.
                    Devolvé SOLO un JSON array de strings (sin markdown): ["frase1","frase2",...]
                    """.formatted(scenario.getProductContext(), scenario.getClientPersona());
            default -> throw new IllegalArgumentException("Sección desconocida: " + section);
        };
    }

    private String callGroq(String prompt) {
        Map<String, Object> body = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.8,
                "max_tokens", 2000
        );
        int[] delays = {3000, 6000};
        Exception lastError = null;
        for (int attempt = 0; attempt <= delays.length; attempt++) {
            try {
                Map<?, ?> response = groqClient.post()
                        .uri("/chat/completions")
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();
                List<?> choices = (List<?>) response.get("choices");
                Map<?, ?> message = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
                return ((String) message.get("content")).trim();
            } catch (Exception e) {
                lastError = e;
                if (attempt < delays.length) {
                    log.warn("Groq intento {} fallido: {}. Reintentando en {}ms...", attempt + 1, e.getMessage(), delays[attempt]);
                    try { Thread.sleep(delays[attempt]); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw new RuntimeException("Groq no disponible: " + (lastError != null ? lastError.getMessage() : "error desconocido"));
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> parseWithRetry(String originalPrompt, String rawJson) {
        try {
            return objectMapper.readValue(sanitizeJson(rawJson), Map.class);
        } catch (Exception e) {
            log.warn("JSON malformado en primer intento ({}), reintentando generación...", e.getMessage());
            String retried = callGroq(originalPrompt);
            try {
                return objectMapper.readValue(sanitizeJson(retried), Map.class);
            } catch (Exception e2) {
                throw new RuntimeException("La IA generó JSON inválido después de 2 intentos: " + e2.getMessage());
            }
        }
    }

    private String sanitizeJson(String raw) {
        if (raw == null) return null;
        String s = raw.strip();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("```\\s*$", "").strip();
        }
        return s;
    }

    private String asString(Map<?, ?> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    private String toJsonString(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return value.toString();
        }
    }
}
