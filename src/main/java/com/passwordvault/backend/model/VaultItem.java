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
@Table(name = "vault_items")
public class VaultItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vault_item_id")
    private Long vaultItemId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "username", length = 255)
    private String username;

    @Lob
    @Column(name = "notes_encrypted", columnDefinition = "BLOB")
    private byte[] notesEncrypted;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // ACTIVE, DELETED

    @Column(name = "current_version", nullable = false)
    private int currentVersion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = "ACTIVE";
        if (this.currentVersion == 0) this.currentVersion = 1;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

