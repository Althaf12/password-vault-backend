package com.passwordvault.backend.service;

import com.passwordvault.backend.model.VaultAuditLog;
import com.passwordvault.backend.model.VaultViewSession;
import com.passwordvault.backend.repository.VaultAuditLogRepository;
import com.passwordvault.backend.repository.VaultViewSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Handles all audit event recording for the vault.
 * Never logs decrypted passwords.
 *
 * Action types:
 *   CREATE         - vault item created
 *   VIEW           - vault item metadata viewed
 *   VIEW_PASSWORD  - encrypted password/version retrieved (client decrypts client-side)
 *   UPDATE         - vault item metadata updated
 *   ADD_VERSION    - new encrypted password version added
 *   DELETE         - vault item soft-deleted (moved to trash)
 *   RESTORE        - vault item restored from trash
 *   PERMANENT_DELETE - vault item permanently deleted
 */
@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    private final VaultAuditLogRepository auditLogRepository;
    private final VaultViewSessionRepository viewSessionRepository;

    public AuditService(VaultAuditLogRepository auditLogRepository,
                        VaultViewSessionRepository viewSessionRepository) {
        this.auditLogRepository = auditLogRepository;
        this.viewSessionRepository = viewSessionRepository;
    }

    public VaultAuditLog log(String userId, Long vaultItemId, String actionType,
                             String ipAddress, String userAgent, String status) {
        VaultAuditLog log = new VaultAuditLog();
        log.setUserId(userId);
        log.setVaultItemId(vaultItemId);
        log.setActionType(actionType);
        log.setIpAddress(ipAddress);
        log.setUserAgent(truncate(userAgent, 500));
        log.setActionTimestamp(LocalDateTime.now());
        log.setStatus(status);
        logger.info("Audit: user={} action={} vaultItemId={} status={}", userId, actionType, vaultItemId, status);
        return auditLogRepository.save(log);
    }

    public VaultAuditLog logSuccess(String userId, Long vaultItemId, String actionType,
                                    String ipAddress, String userAgent) {
        return log(userId, vaultItemId, actionType, ipAddress, userAgent, "SUCCESS");
    }

    public VaultAuditLog logFailure(String userId, Long vaultItemId, String actionType,
                                    String ipAddress, String userAgent) {
        return log(userId, vaultItemId, actionType, ipAddress, userAgent, "FAILURE");
    }

    /**
     * Records a password view session (for enterprise-level tracking).
     */
    public VaultViewSession recordViewSession(String userId, Long vaultItemId, String ipAddress) {
        VaultViewSession session = new VaultViewSession();
        session.setUserId(userId);
        session.setVaultItemId(vaultItemId);
        session.setSessionIp(ipAddress);
        session.setViewedAt(LocalDateTime.now());
        logger.info("ViewSession: user={} vaultItemId={} ip={}", userId, vaultItemId, ipAddress);
        return viewSessionRepository.save(session);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}

