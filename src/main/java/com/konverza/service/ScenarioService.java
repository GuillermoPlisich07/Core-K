package com.konverza.service;

import com.konverza.dto.ScenarioRequest;
import com.konverza.entity.Scenario;
import com.konverza.repository.ScenarioRepository;
import lombok.RequiredArgsConstructor;
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

    public Scenario update(UUID id, ScenarioRequest req) {
        Scenario s = findById(id);
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
