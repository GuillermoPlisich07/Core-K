package com.konverza.scenarios.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.konverza.empresa.entity.Empresa;
import com.konverza.productos.entity.Producto;
import com.konverza.shared.enums.Industry;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.Hibernate;

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


    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "objections_guide", columnDefinition = "TEXT")
    private String objectionsGuide;


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

    @Column(name = "owner_name")
    private String ownerName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ClientPersona { ANGRY, DIFFICULT, INDIFFERENT, DEMANDING }
    public enum Difficulty { EASY, MEDIUM, HARD }

    @Transient
    @JsonProperty("compiledSystemPrompt")
    public String getCompiledSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("<CONTEXT>\n");
        sb.append("[Perfil del Cliente]\n");
        sb.append("Arquetipo: ").append(this.clientPersona != null ? this.clientPersona : "Indefinido")
          .append(" | Dificultad: ").append(this.difficulty != null ? this.difficulty : "Indefinida").append("\n");
        sb.append("Comportamiento: ").append(this.systemPrompt != null ? this.systemPrompt : "").append("\n\n");
        
        sb.append("[Contexto de Venta]\n");
        sb.append("Vendedor: ").append(this.vendedorRol != null ? this.vendedorRol : "Representante de Ventas")
          .append(" de la empresa ");
        if (this.empresa != null && Hibernate.isInitialized(this.empresa)) {
            sb.append(this.empresa.getName());
            if (this.industry != null) {
                sb.append(" (").append(this.industry).append(")");
            }
            sb.append(". ");
            if (this.empresa.getContext() != null) {
                sb.append(this.empresa.getContext());
            }
        } else {
            sb.append("Tu Empresa");
            if (this.industry != null) {
                sb.append(" (").append(this.industry).append(")");
            }
            sb.append(".");
        }
        sb.append("\nObjetivo del Vendedor: ").append(this.escenarioObjetivo != null ? this.escenarioObjetivo : "Establecer relación comercial").append("\n");
        
        sb.append("Producto: ");
        boolean hasEntityProduct = (this.producto != null && Hibernate.isInitialized(this.producto));
        if (hasEntityProduct) {
            sb.append(this.producto.getName()).append(". ");
            if (this.producto.getDescription() != null && !this.producto.getDescription().isBlank()) {
                sb.append(this.producto.getDescription()).append(" ");
            }
            if (this.producto.getPriceRange() != null && !this.producto.getPriceRange().isBlank()) {
                sb.append("Precio: ").append(this.producto.getPriceRange().trim()).append(". ");
            }
            if (this.producto.getKeyDifferentiator() != null && !this.producto.getKeyDifferentiator().isBlank()) {
                sb.append("Diferencial: ").append(this.producto.getKeyDifferentiator().trim()).append(". ");
            }
            if (this.producto.getTags() != null && !this.producto.getTags().isEmpty()) {
                sb.append("Tags: ").append(String.join(", ", this.producto.getTags())).append(". ");
            }
            sb.append("\n");
            if (this.producto.getPaymentInfo() != null && !this.producto.getPaymentInfo().isBlank()) {
                sb.append("Condiciones comerciales: ").append(this.producto.getPaymentInfo().trim()).append("\n");
            }
        } else {
            sb.append("Tu Producto/Servicio.\n");
        }
        sb.append("</CONTEXT>\n\n");

        sb.append("<REQUEST>\n");
        sb.append("Actúa estrictamente como el cliente descrito en una simulación de ventas B2B de máximo ")
          .append(this.maxDurationMinutes != null ? this.maxDurationMinutes : 30)
          .append(" minutos. Nunca reveles que eres una IA.\n");
        sb.append("</REQUEST>\n\n");

        sb.append("<EXPLANATION>\n");
        sb.append("Guíate por estas reglas e incorpóralas orgánicamente si el vendedor no las resuelve:\n");
        sb.append("- OBJECIONES: ").append(this.objectionsGuide != null ? this.objectionsGuide : "Ninguna específica.").append("\n");
        sb.append("- PREGUNTAS FRECUENTES: ").append(this.faq != null ? this.faq : "Ninguna específica.").append("\n");
        sb.append("- FRASES PROHIBIDAS: Si el vendedor dice ").append(this.forbiddenPhrases != null ? this.forbiddenPhrases : "ninguna específica")
          .append(", reacciona negativamente (pierde interés, interrumpe o moléstate).\n");
        sb.append("</EXPLANATION>\n\n");

        sb.append("<STRUCTURE>\n");
        sb.append("Interactúa turno a turno. Respuestas breves (1 a 3 oraciones), directas y naturales. Cero formato markdown (sin listas ni negritas). No des feedback al final, tu único rol es actuar.\n");
        sb.append("</STRUCTURE>\n\n");

        sb.append("<TONE>\n");
        sb.append("Español rioplatense (voseo). Tono estrictamente adaptado a tu comportamiento y arquetipo (")
          .append(this.clientPersona != null ? this.clientPersona : "Indefinido").append(").\n");
        sb.append("</TONE>");

        return sb.toString();
    }
}
