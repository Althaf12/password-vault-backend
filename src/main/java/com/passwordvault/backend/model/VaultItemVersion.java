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
@Table(name = "vault_item_versions",
    uniqueConstraints = @UniqueConstraint(name = "uk_item_version", columnNames = {"vault_item_id", "version_number"}))
public class VaultItemVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "version_id")
    private Long versionId;

    @Column(name = "vault_item_id", nullable = false)
    private Long vaultItemId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Lob
    @Column(name = "password_encrypted", nullable = false, columnDefinition = "BLOB")
    private byte[] passwordEncrypted;

    @Column(name = "encryption_iv", nullable = false, columnDefinition = "VARBINARY(255)")
    private byte[] encryptionIv;

    @Column(name = "encryption_algo", nullable = false, length = 50)
    private String encryptionAlgo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.encryptionAlgo == null) this.encryptionAlgo = "AES-256-GCM";
    }
}

