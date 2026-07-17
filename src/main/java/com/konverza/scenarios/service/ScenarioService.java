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
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final EmpresaRepository empresaRepository;
    private final ProductoRepository productoRepository;

    public List<Scenario> findAll() {
        return scenarioRepository.findAll();
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Scenario findById(UUID id) {
        Scenario scenario = scenarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Escenario no encontrado: " + id));
        org.hibernate.Hibernate.initialize(scenario.getEmpresa());
        org.hibernate.Hibernate.initialize(scenario.getProducto());
        return scenario;
    }


    @org.springframework.transaction.annotation.Transactional
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
    @org.springframework.transaction.annotation.Transactional
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

    @org.springframework.transaction.annotation.Transactional
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
