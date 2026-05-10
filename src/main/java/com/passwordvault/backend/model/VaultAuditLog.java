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
@Table(name = "vault_audit_logs")
public class VaultAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "vault_item_id")
    private Long vaultItemId;

    /**
     * Action types: CREATE, VIEW, VIEW_PASSWORD, UPDATE, ADD_VERSION,
     * DELETE, RESTORE, PERMANENT_DELETE
     */
    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "action_timestamp", nullable = false)
    private LocalDateTime actionTimestamp;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // SUCCESS, FAILURE

    @PrePersist
    public void prePersist() {
        if (this.actionTimestamp == null) this.actionTimestamp = LocalDateTime.now();
        if (this.status == null) this.status = "SUCCESS";
    }
}

