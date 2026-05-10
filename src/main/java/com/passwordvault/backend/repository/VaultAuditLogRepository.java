package com.passwordvault.backend.repository;

import com.passwordvault.backend.model.VaultAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VaultAuditLogRepository extends JpaRepository<VaultAuditLog, Long> {

    List<VaultAuditLog> findByUserIdOrderByActionTimestampDesc(String userId);

    List<VaultAuditLog> findByUserIdAndVaultItemIdOrderByActionTimestampDesc(String userId, Long vaultItemId);
}

