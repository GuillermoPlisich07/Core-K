package com.konverza.scenarios.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.konverza.auth.entity.User;
import com.konverza.empresa.entity.Empresa;
import com.konverza.productos.entity.Producto;
import com.konverza.shared.enums.Industry;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "scenarios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_persona", nullable = false)
    private ClientPersona clientPersona;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Column(name = "product_context", columnDefinition = "TEXT")
    private String productContext;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "objections_guide", columnDefinition = "TEXT")
    private String objectionsGuide;

    @Column(name = "payment_info", columnDefinition = "TEXT")
    private String paymentInfo;

    @Column(columnDefinition = "TEXT")
    private String faq;

    @Column(name = "avatar_voice_id")
    private String avatarVoiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "industry")
    private Industry industry;

    @Column(name = "max_duration_minutes")
    private Integer maxDurationMinutes;

    @Column(name = "evaluation_weights", columnDefinition = "TEXT")
    private String evaluationWeights;

    @Column(name = "forbidden_phrases", columnDefinition = "TEXT")
    private String forbiddenPhrases;

    @Column(name = "vendedor_rol")
    private String vendedorRol;

    @Column(name = "escenario_objetivo", columnDefinition = "TEXT")
    private String escenarioObjetivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Producto producto;

    /** Convenience UUID exposed in JSON for the frontend (← replaces full empresa object). */
    @JsonProperty("empresaId")
    public UUID getEmpresaId() { return empresa != null ? empresa.getId() : null; }

    /** Convenience UUID exposed in JSON for the frontend (← replaces full producto object). */
    @JsonProperty("productoId")
    public UUID getProductoId() { return producto != null ? producto.getId() : null; }

    @Column(name = "created_by")
    private String createdBy;

    /**
     * Real, server-verified owner of an Escenario Rápido — set from
     * CurrentUser.id() at creation, never from client input
     * (scenario-privacy-and-lifecycle). Escenarios Completos (MANUAL) leave
     * this null; they stay creator-agnostic and company-wide by design.
     * EAGER (unlike empresa/producto below): getOwnerName() reads
     * firstName/lastName, not just the FK id, and open-in-view is disabled,
     * so a LAZY proxy would throw once accessed during response
     * serialization, after the request's Hibernate session has closed.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_user_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User createdByUser;

    /** Read-only display name derived from the real owner — replaces the old client-writable ownerName column. */
    @JsonProperty("createdByUserId")
    public UUID getCreatedByUserId() { return createdByUser != null ? createdByUser.getId() : null; }

    @JsonProperty("ownerName")
    public String getOwnerName() {
        if (createdByUser == null) return null;
        String fullName = (createdByUser.getFirstName() + " " + createdByUser.getLastName()).trim();
        return !fullName.isEmpty() ? fullName : createdByUser.getEmail();
    }

    /**
     * columnDefinition carries an explicit DB-level DEFAULT so
     * ddl-auto=update's ALTER TABLE ADD COLUMN succeeds against the existing
     * scenarios table (Postgres rejects adding a NOT NULL column with no
     * default to a non-empty table) — same pattern as User.profileCompleted.
     */
    @Column(nullable = false, columnDefinition = "boolean not null default true")
    @Builder.Default
    private boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ClientPersona { ANGRY, DIFFICULT, INDIFFERENT, DEMANDING }
    public enum Difficulty { EASY, MEDIUM, HARD }
}
