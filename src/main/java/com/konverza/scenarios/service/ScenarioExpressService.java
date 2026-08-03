package com.konverza.scenarios.service;

import com.konverza.auth.entity.User;
import com.konverza.auth.exception.UserNotFoundException;
import com.konverza.auth.repository.UserRepository;
import com.konverza.auth.security.CurrentUser;
import com.konverza.scenarios.dto.ScenarioExpressRequest;
import com.konverza.scenarios.entity.Scenario;
import com.konverza.scenarios.repository.ScenarioRepository;
import com.konverza.productos.entity.Producto;
import com.konverza.productos.repository.ProductoRepository;
import com.konverza.productos.exception.ProductoNotFoundException;
import com.konverza.empresa.entity.Empresa;
import com.konverza.empresa.repository.EmpresaRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;

@Slf4j
@Service
public class ScenarioExpressService {

    private final WebClient groqClient;
    private final ScenarioRepository scenarioRepository;
    private final UserRepository userRepository;
    private final ScenarioService scenarioService;
    private final ObjectMapper objectMapper;
    private final ProductoRepository productoRepository;
    private final EmpresaRepository empresaRepository;

    public ScenarioExpressService(
            @Qualifier("groqClient") WebClient groqClient,
            ScenarioRepository scenarioRepository,
            UserRepository userRepository,
            ScenarioService scenarioService,
            ObjectMapper objectMapper,
            ProductoRepository productoRepository,
            EmpresaRepository empresaRepository) {
        this.groqClient = groqClient;
        this.scenarioRepository = scenarioRepository;
        this.userRepository = userRepository;
        this.scenarioService = scenarioService;
        this.objectMapper = objectMapper;
        this.productoRepository = productoRepository;
        this.empresaRepository = empresaRepository;
    }

    public Scenario generateAndSave(ScenarioExpressRequest req) {
        Producto producto = productoRepository.findById(req.getProductoId())
                .orElseThrow(() -> new ProductoNotFoundException(req.getProductoId()));
        
        Empresa empresa = empresaRepository.findAll().stream().findFirst().orElse(null);

        User owner = userRepository.findById(CurrentUser.id())
                .orElseThrow(() -> new UserNotFoundException(CurrentUser.id()));

        String vendedorName = (owner.getFirstName() + " " + owner.getLastName()).trim();
        if (vendedorName.isEmpty()) vendedorName = "Vendedor";

        String sellerRole = req.getVendedorRol() != null && !req.getVendedorRol().isBlank() ? req.getVendedorRol().trim() : "Representante de Ventas";

        String prompt = buildExpressGenerationPrompt(req, producto, empresa, vendedorName, sellerRole);
        String rawJson = callGroq(prompt);
        Map<?, ?> parsed = parseWithRetry(prompt, rawJson);

        Scenario scenario = Scenario.builder()
                .name(req.getName())
                .description(req.getDescription())
                .industries(empresa != null ? new HashSet<>(empresa.getIndustries()) : new HashSet<>())
                .clientPersona(req.getClientPersona())
                .difficulty(req.getDifficulty())
                .maxDurationMinutes(15)
                .createdBy("EXPRESS_AI")
                .createdByUser(owner)
                .vendedorRol(sellerRole)
                .empresa(empresa)
                .producto(producto)
                .systemPrompt(asString(parsed, "system_prompt"))
                .objectionsGuide(toJsonString(parsed.get("objections_guide")))
                .faq(toJsonString(parsed.get("faq")))
                .forbiddenPhrases(toJsonString(parsed.get("forbidden_phrases")))
                .voiceId(asString(parsed, "voice_id"))
                .avatarId(asString(parsed, "avatar_id"))
                .build();

        List<Map<String, Object>> phasesData = (List<Map<String, Object>>) parsed.get("phases");
        if (phasesData != null) {
            int order = 1;
            for (Map<String, Object> pd : phasesData) {
                com.konverza.scenarios.entity.ScenarioPhase phase = com.konverza.scenarios.entity.ScenarioPhase.builder()
                        .name((String) pd.get("name"))
                        .description((String) pd.get("description"))
                        .estimatedTimeMinutes(pd.get("estimatedTimeMinutes") instanceof Number ? ((Number) pd.get("estimatedTimeMinutes")).intValue() : null)
                        .orderIndex(order++)
                        .scenario(scenario)
                        .build();
                scenario.getPhases().add(phase);
            }
        }

        return scenarioRepository.save(scenario);
    }

    /**
     * Shared by the Express review screen (EMPLOYEE, own AI-generated draft)
     * and the Detallado edit screen (ADMIN) — see ScenarioService.update's
     * javadoc. Same ownership rule: EMPLOYEE only on EXPRESS_AI scenarios.
     * Goes through ScenarioService.findById so the same visibility scoping
     * (creator-only for quick scenarios, 404 for others) applies here too
     * (scenario-privacy-and-lifecycle).
     */
    public String regenerateSection(UUID scenarioId, String section) {
        Scenario scenario = scenarioService.findById(scenarioId);
        if ("EMPLOYEE".equals(CurrentUser.role()) && !"EXPRESS_AI".equals(scenario.getCreatedBy())) {
            throw new AccessDeniedException("Solo se pueden regenerar secciones de escenarios propios creados con el flujo Express");
        }

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

    private String buildExpressGenerationPrompt(ScenarioExpressRequest req, Producto producto, Empresa empresa, String vendedorName, String vendedorRol) {
        String empresaName = (empresa != null) ? empresa.getName() : "Tu Empresa";
        
        return """
                Sos un experto en entrenamiento de vendedores para el mercado uruguayo y argentino.
                Generá el contenido de un escenario de roleplay para entrenamiento de ventas.

                Parámetros del escenario:
                - Empresa: %s
                - Vendedor: %s (Rol del vendedor: %s)
                - Producto/servicio: %s
                - Descripción del producto: %s
                - Precio: %s
                - Diferencial clave: %s
                - Tipo de cliente: %s (ANGRY=muy enojado, DIFFICULT=difícil, INDIFFERENT=indiferente, DEMANDING=muy exigente)
                - Dificultad: %s

                Generá un JSON con exactamente esta estructura (sin markdown, sin backticks):
                {
                  "system_prompt": "Instrucción completa para el avatar que juega el rol del cliente. En español rioplatense (voseo). Debe incluir: personalidad, historia de fondo, motivaciones, cómo reacciona a diferentes situaciones. Mínimo 200 palabras.",
                  "product_context": "Contexto del producto que el vendedor puede usar. Info técnica, beneficios, casos de uso. Mínimo 150 palabras.",
                  "phases": [
                    { "name": "nombre de la fase (ej: Contacto)", "description": "descripción de lo que el vendedor debe hacer", "estimatedTimeMinutes": 3 }
                  ],
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
                empresaName, vendedorName, vendedorRol,
                producto.getName(), producto.getDescription() != null ? producto.getDescription() : "",
                producto.getPriceRange() != null ? producto.getPriceRange() : "",
                producto.getKeyDifferentiator() != null ? producto.getKeyDifferentiator() : "",
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
                    """.formatted(scenario.getClientPersona(), scenario.getDifficulty(), "");
            case "objections_guide" -> """
                    Generá 5 objeciones realistas para el siguiente escenario de ventas.
                    Tipo de cliente: %s. Producto/contexto: %s
                    Devolvé SOLO un JSON array (sin markdown):
                    [{"trigger":"...","objection":"...","hint":"..."}]
                    """.formatted(scenario.getClientPersona(), "");
            case "faq" -> """
                    Generá 8 preguntas frecuentes para el siguiente escenario de ventas.
                    Producto/contexto: %s
                    Devolvé SOLO un JSON array (sin markdown):
                    [{"question":"...","answer":"..."}]
                    """.formatted("");
            case "forbidden_phrases" -> """
                    Generá 6 frases que un vendedor inexperto NO debe decir al vender este producto.
                    Producto/contexto: %s. Tipo de cliente: %s.
                    Devolvé SOLO un JSON array de strings (sin markdown): ["frase1","frase2",...]
                    """.formatted("", scenario.getClientPersona());
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
