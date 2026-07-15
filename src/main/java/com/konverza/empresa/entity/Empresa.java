package com.konverza.empresa.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.konverza.shared.enums.Industry;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Singleton company-context record — at most one row ever exists
 * (add-users-empresa-profile). See {@link com.konverza.empresa.service.EmpresaService}
 * for the get-or-404 / upsert shape this singleton-ness implies.
 */
@Entity
@Table(name = "empresa")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String context;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String vision;

    @Column(columnDefinition = "TEXT")
    private String objective;

    /** A company may operate in more than one industry — see design.md's "Company industries" decision. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "empresa_industries", joinColumns = @JoinColumn(name = "empresa_id"))
    @Column(name = "industry")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<Industry> industries = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
