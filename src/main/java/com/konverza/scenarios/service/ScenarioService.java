package com.konverza.scenarios.service;

import com.konverza.auth.security.CurrentUser;
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

    public List<Scenario> findAll() {
        return scenarioRepository.findAll();
    }

    public Scenario findById(UUID id) {
        return scenarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Escenario no encontrado: " + id));
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
                .createdBy("MANUAL")
                .build();
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
        return scenarioRepository.save(s);
    }

    public void delete(UUID id) { scenarioRepository.deleteById(id); }
}
