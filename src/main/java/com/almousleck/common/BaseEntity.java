package com.almousleck.common;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant created;

    @Column(name = "updated_at")
    private Instant updated;

    @PrePersist
    protected void onCreate() {
        this.created = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updated = Instant.now();
    }

}
