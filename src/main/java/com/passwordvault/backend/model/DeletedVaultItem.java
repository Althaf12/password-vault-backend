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
@Table(name = "deleted_vault_items")
public class DeletedVaultItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deleted_id")
    private Long deletedId;

    @Column(name = "vault_item_id", nullable = false)
    private Long vaultItemId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "deleted_at", nullable = false)
    private LocalDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        if (this.deletedAt == null) this.deletedAt = LocalDateTime.now();
    }
}

