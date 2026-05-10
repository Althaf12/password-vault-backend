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
@Table(name = "vault_view_sessions")
public class VaultViewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "vault_item_id", nullable = false)
    private Long vaultItemId;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    @Column(name = "session_ip", length = 45)
    private String sessionIp;

    @PrePersist
    public void prePersist() {
        if (this.viewedAt == null) this.viewedAt = LocalDateTime.now();
    }
}

