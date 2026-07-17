package com.konverza.scenarios.service;

import com.konverza.auth.security.CurrentUser;
import com.konverza.empresa.entity.Empresa;
import com.konverza.empresa.repository.EmpresaRepository;
import com.konverza.productos.entity.Producto;
import com.konverza.productos.repository.ProductoRepository;
import com.konverza.scenarios.dto.ScenarioRequest;
import com.konverza.scenarios.entity.Scenario;
import com.konverza.scenarios.exception.InvalidScenarioTypeException;
import com.konverza.scenarios.exception.ScenarioNotFoundException;
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

    private static final String EXPRESS_ORIGIN = "EXPRESS_AI";
    private static final String MANUAL_ORIGIN = "MANUAL";

    private final ScenarioRepository scenarioRepository;
    private final EmpresaRepository empresaRepository;
    private final ProductoRepository productoRepository;

    /**
     * Scoped per scenario-privacy-and-lifecycle: EMPLOYEE/EXEC see their own
     * enabled Escenarios Rápidos plus every enabled Escenario Completo; ADMIN
     * sees every Escenario Completo (any enabled state, needed to manage
     * them) plus their own enabled Escenarios Rápidos. No role is shown
     * another user's Escenario Rápido.
     */
    public List<Scenario> findAll() {
        return scenarioRepository.findAll().stream()
                .filter(this::isVisibleToCurrentUser)
                .toList();
    }

    private boolean isVisibleToCurrentUser(Scenario scenario) {
        if (MANUAL_ORIGIN.equals(scenario.getCreatedBy())) {
            return "ADMIN".equals(CurrentUser.role()) || scenario.isEnabled();
        }
        UUID ownerId = scenario.getCreatedByUser() != null ? scenario.getCreatedByUser().getId() : null;
        return scenario.isEnabled() && ownerId != null && ownerId.equals(currentUserIdOrNull());
    }

    /**
     * A caller whose identity can't be resolved never matches a real owner
     * id, so this fails closed (not visible) rather than propagating the
     * parse failure — CurrentUser.id() only fails to parse for a malformed
     * principal, which never happens for a real JWT-authenticated request.
     */
    private UUID currentUserIdOrNull() {
        try {
            return CurrentUser.id();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * A scenario the current user can't see is indistinguishable from one
     * that doesn't exist — both resolve to ScenarioNotFoundException (404) —
     * so a non-owner can't tell "not found" from "not yours" via direct-ID
     * access (scenario-privacy-and-lifecycle).
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Scenario findById(UUID id) {
        Scenario scenario = scenarioRepository.findById(id)
                .orElseThrow(() -> new ScenarioNotFoundException(id));
        if (!isVisibleToCurrentUser(scenario)) {
            throw new ScenarioNotFoundException(id);
        }
        org.hibernate.Hibernate.initialize(scenario.getEmpresa());
        org.hibernate.Hibernate.initialize(scenario.getProducto());
        return scenario;
    }


    @org.springframework.transaction.annotation.Transactional
    public Scenario create(ScenarioRequest req) {
        Scenario scenario = Scenario.builder()
                .name(req.getName()).description(req.getDescription())
                .clientPersona(req.getClientPersona()).difficulty(req.getDifficulty())
                .systemPrompt(req.getSystemPrompt())
                .objectionsGuide(req.getObjectionsGuide())
                .faq(req.getFaq()).avatarVoiceId(req.getAvatarVoiceId())
                .industries(req.getIndustries())
                .maxDurationMinutes(req.getMaxDurationMinutes() != null ? req.getMaxDurationMinutes() : 30)
                .evaluationWeights(req.getEvaluationWeights())
                .forbiddenPhrases(req.getForbiddenPhrases())
                .vendedorRol(req.getVendedorRol())
                .escenarioObjetivo(req.getEscenarioObjetivo())
                .createdBy(MANUAL_ORIGIN)
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
        if ("EMPLOYEE".equals(CurrentUser.role()) && !EXPRESS_ORIGIN.equals(s.getCreatedBy())) {
            throw new AccessDeniedException("Solo se pueden editar escenarios propios creados con el flujo Express");
        }
        s.setName(req.getName()); s.setDescription(req.getDescription());
        s.setClientPersona(req.getClientPersona()); s.setDifficulty(req.getDifficulty());
        s.setSystemPrompt(req.getSystemPrompt());
        s.setObjectionsGuide(req.getObjectionsGuide());
        s.setFaq(req.getFaq()); s.setAvatarVoiceId(req.getAvatarVoiceId());
        s.setIndustries(req.getIndustries());
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

    /**
     * Admin activate/deactivate control for Escenarios Completos only —
     * quick-scenario lifecycle is fully automatic (expiration job), so
     * admin control over them is explicitly out of scope
     * (scenario-privacy-and-lifecycle).
     */
    @org.springframework.transaction.annotation.Transactional
    public Scenario setEnabled(UUID id, boolean enabled) {
        Scenario scenario = findById(id);
        if (!MANUAL_ORIGIN.equals(scenario.getCreatedBy())) {
            throw new InvalidScenarioTypeException("Solo se puede activar/desactivar Escenarios Completos");
        }
        scenario.setEnabled(enabled);
        return scenarioRepository.save(scenario);
    }

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
