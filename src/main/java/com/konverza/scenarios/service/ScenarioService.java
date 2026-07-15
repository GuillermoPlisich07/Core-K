package com.konverza.scenarios.service;

import com.konverza.auth.security.CurrentUser;
import com.konverza.empresa.entity.Empresa;
import com.konverza.empresa.repository.EmpresaRepository;
import com.konverza.productos.entity.Producto;
import com.konverza.productos.repository.ProductoRepository;
import com.konverza.scenarios.dto.ScenarioRequest;
import com.konverza.scenarios.entity.Scenario;
import com.konverza.scenarios.repository.ScenarioRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final EmpresaRepository empresaRepository;
    private final ProductoRepository productoRepository;

    public List<Scenario> findAll() {
        return scenarioRepository.findAll();
    }

    public Scenario findById(UUID id) {
        return scenarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Escenario no encontrado: " + id));
    }

    /**
     * Dynamically assembles the full system prompt from the structured fields of
     * the scenario. This replaces the raw {@code systemPrompt} string as the source
     * of truth for what is delivered to the AI engine (FastAPI / Tavus), while the
     * {@code systemPrompt} field now exclusively stores the client persona's
     * behaviour profile text.
     */
    public String compileSystemPrompt(Scenario scenario) {
        StringBuilder sb = new StringBuilder();
        sb.append("<INSTRUCCIONES_DEL_SISTEMA>\n");
        sb.append("Actúas como un simulador de entrenamiento B2B de alta fidelidad. Tu rol es interpretar a un cliente potencial interactuando con un representante de ventas, respetando estrictamente los siguientes contextos.\n\n");

        // Contexto de la Empresa Vendedora
        sb.append("<CONTEXTO_DE_LA_EMPRESA_VENDEDORA>\n");
        if (scenario.getEmpresa() != null) {
            Empresa emp = scenario.getEmpresa();
            sb.append("Nombre de la empresa: ").append(emp.getName()).append("\n");
            if (scenario.getIndustry() != null) {
                sb.append("Industria: ").append(scenario.getIndustry()).append("\n");
            }
            if (emp.getContext() != null && !emp.getContext().isBlank()) {
                sb.append("Posicionamiento: ").append(emp.getContext()).append("\n");
            }
        } else {
            if (scenario.getIndustry() != null) {
                sb.append("Industria: ").append(scenario.getIndustry()).append("\n");
            }
        }
        sb.append("\n");

        // Contexto de Producto o Servicio
        sb.append("<CONTEXTO_DEL_PRODUCTO_O_SERVICIO>\n");
        if (scenario.getProducto() != null) {
            Producto prod = scenario.getProducto();
            sb.append("Producto/Servicio en foco: ").append(prod.getName()).append("\n");
            if (prod.getDescription() != null && !prod.getDescription().isBlank()) {
                sb.append("Descripción: ").append(prod.getDescription()).append("\n");
            }
        }
        if (scenario.getProductContext() != null && !scenario.getProductContext().isBlank()) {
            sb.append("Descripción y valor: ").append(scenario.getProductContext()).append("\n");
        }
        if (scenario.getPaymentInfo() != null && !scenario.getPaymentInfo().isBlank()) {
            sb.append("Condiciones comerciales: ").append(scenario.getPaymentInfo()).append("\n");
        }
        sb.append("\n");

        // Contexto del Vendedor y Escenario
        sb.append("<CONTEXTO_DEL_VENDEDOR_Y_ESCENARIO>\n");
        sb.append("El usuario con el que hablas es un: ")
          .append(scenario.getVendedorRol() != null ? scenario.getVendedorRol() : "Representante de Ventas")
          .append("\n");
        sb.append("Objetivo del escenario: ")
          .append(scenario.getEscenarioObjetivo() != null ? scenario.getEscenarioObjetivo() : "Establecer relación comercial")
          .append("\n");
        if (scenario.getMaxDurationMinutes() != null) {
            sb.append("Duración máxima: ").append(scenario.getMaxDurationMinutes()).append(" minutos.\n");
        }
        sb.append("\n");

        // Persona del Cliente
        sb.append("<TU_PERSONA_COMO_CLIENTE>\n");
        if (scenario.getDifficulty() != null) {
            sb.append("Dificultad: ").append(scenario.getDifficulty());
        }
        if (scenario.getClientPersona() != null) {
            sb.append(" | Arquetipo: ").append(scenario.getClientPersona());
        }
        sb.append("\n");
        if (scenario.getSystemPrompt() != null && !scenario.getSystemPrompt().isBlank()) {
            sb.append("Perfil de comportamiento: ").append(scenario.getSystemPrompt()).append("\n");
        }
        sb.append("\n");

        // Guardrails
        sb.append("<REGLAS_DE_INTERACCION_Y_GUARDRAILS>\n");
        sb.append("1. INMERSIÓN: Mantén el personaje en todo momento. Habla en primera persona. Nunca reveles que eres una IA.\n");
        if (scenario.getObjectionsGuide() != null && !scenario.getObjectionsGuide().isBlank()) {
            sb.append("2. OBJECIONES: Debes introducir naturalmente las siguientes objeciones si el vendedor no las previene: ")
              .append(scenario.getObjectionsGuide()).append("\n");
        }
        if (scenario.getForbiddenPhrases() != null && !scenario.getForbiddenPhrases().isBlank()) {
            sb.append("3. PENALIZACIONES: Si el vendedor utiliza frases prohibidas como ")
              .append(scenario.getForbiddenPhrases())
              .append(", debes reaccionar negativamente (perder el interés, interrumpir o molestarte).\n");
        }
        if (scenario.getFaq() != null && !scenario.getFaq().isBlank()) {
            sb.append("4. PREGUNTAS FRECUENTES: Si el contexto lo permite, haz estas preguntas: ")
              .append(scenario.getFaq()).append("\n");
        }
        sb.append("</INSTRUCCIONES_DEL_SISTEMA>");

        return sb.toString();
    }

    public Scenario create(ScenarioRequest req) {
        Scenario scenario = Scenario.builder()
                .name(req.getName()).description(req.getDescription())
                .clientPersona(req.getClientPersona()).difficulty(req.getDifficulty())
                .productContext(req.getProductContext()).systemPrompt(req.getSystemPrompt())
                .objectionsGuide(req.getObjectionsGuide()).paymentInfo(req.getPaymentInfo())
                .faq(req.getFaq()).avatarVoiceId(req.getAvatarVoiceId())
                .industry(req.getIndustry())
                .maxDurationMinutes(req.getMaxDurationMinutes() != null ? req.getMaxDurationMinutes() : 30)
                .evaluationWeights(req.getEvaluationWeights())
                .forbiddenPhrases(req.getForbiddenPhrases())
                .vendedorRol(req.getVendedorRol())
                .escenarioObjetivo(req.getEscenarioObjetivo())
                .createdBy("MANUAL")
                .build();

        resolveRelations(scenario, req);
        return scenarioRepository.save(scenario);
    }

    /**
     * PUT /api/scenarios/{id} is shared by both the Detallado edit screen
     * (ADMIN) and the Express review "Guardar" step (EMPLOYEE saving their
     * own AI-generated draft) — see docs/modulos/frontend.md's
     * ScenarioExpressScreen section. EMPLOYEE may only save scenarios created
     * via that flow (add-rbac-permission-matrix); ADMIN can edit any.
     */
    public Scenario update(UUID id, ScenarioRequest req) {
        Scenario s = findById(id);
        if ("EMPLOYEE".equals(CurrentUser.role()) && !"EXPRESS_AI".equals(s.getCreatedBy())) {
            throw new AccessDeniedException("Solo se pueden editar escenarios propios creados con el flujo Express");
        }
        s.setName(req.getName()); s.setDescription(req.getDescription());
        s.setClientPersona(req.getClientPersona()); s.setDifficulty(req.getDifficulty());
        s.setProductContext(req.getProductContext()); s.setSystemPrompt(req.getSystemPrompt());
        s.setObjectionsGuide(req.getObjectionsGuide()); s.setPaymentInfo(req.getPaymentInfo());
        s.setFaq(req.getFaq()); s.setAvatarVoiceId(req.getAvatarVoiceId());
        s.setIndustry(req.getIndustry());
        if (req.getMaxDurationMinutes() != null) s.setMaxDurationMinutes(req.getMaxDurationMinutes());
        s.setEvaluationWeights(req.getEvaluationWeights());
        s.setForbiddenPhrases(req.getForbiddenPhrases());
        s.setVendedorRol(req.getVendedorRol());
        s.setEscenarioObjetivo(req.getEscenarioObjetivo());
        resolveRelations(s, req);
        return scenarioRepository.save(s);
    }

    public void delete(UUID id) { scenarioRepository.deleteById(id); }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Resolves optional empresaId / productoId UUIDs from the request into
     * their JPA-managed entity references. A null UUID clears the relation.
     */
    private void resolveRelations(Scenario scenario, ScenarioRequest req) {
        if (req.getEmpresaId() != null) {
            empresaRepository.findById(req.getEmpresaId()).ifPresent(scenario::setEmpresa);
        } else {
            scenario.setEmpresa(null);
        }
        if (req.getProductoId() != null) {
            productoRepository.findById(req.getProductoId()).ifPresent(scenario::setProducto);
        } else {
            scenario.setProducto(null);
        }
    }
}
