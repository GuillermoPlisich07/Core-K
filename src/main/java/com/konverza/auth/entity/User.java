package com.konverza.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * columnDefinition carries an explicit DB-level DEFAULT so
     * ddl-auto=update's ALTER TABLE ADD COLUMN succeeds against the existing
     * users table (Postgres rejects adding a NOT NULL column with no default
     * to a non-empty table) — same pattern as profileCompleted below.
     */
    @Column(name = "first_name", nullable = false, columnDefinition = "varchar(255) not null default ''")
    @Builder.Default
    private String firstName = "";

    @Column(name = "last_name", nullable = false, columnDefinition = "varchar(255) not null default ''")
    @Builder.Default
    private String lastName = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    private Integer age;

    private String personality;

    @Column(name = "self_description", columnDefinition = "TEXT")
    private String selfDescription;

    @Column(name = "avatar_url")
    private String avatarUrl;

    /**
     * columnDefinition carries an explicit DB-level DEFAULT so
     * ddl-auto=update's ALTER TABLE ADD COLUMN succeeds against the existing
     * users table (Postgres rejects adding a NOT NULL column with no default
     * to a non-empty table) — add-users-empresa-profile.
     */
    @Column(name = "profile_completed", nullable = false, columnDefinition = "boolean not null default false")
    @Builder.Default
    private boolean profileCompleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Role { EMPLOYEE, ADMIN, EXEC }
}
