package com.passwordvault.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_crypto_settings")
public class UserCryptoSettings {

    @Id
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "kdf_algorithm", nullable = false, length = 20)
    private String kdfAlgorithm; // Argon2id / PBKDF2

    @Column(name = "kdf_salt", nullable = false, columnDefinition = "VARBINARY(255)")
    private byte[] kdfSalt;

    @Column(name = "kdf_iterations", nullable = false)
    private int kdfIterations;

    @Column(name = "kdf_memory_kb")
    private Integer kdfMemoryKb;

    @Column(name = "kdf_parallelism")
    private Integer kdfParallelism;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

