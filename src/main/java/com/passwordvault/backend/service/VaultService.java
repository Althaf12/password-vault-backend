package com.passwordvault.backend.service;

import com.passwordvault.backend.dto.*;
import com.passwordvault.backend.exception.BadRequestException;
import com.passwordvault.backend.exception.ResourceNotFoundException;
import com.passwordvault.backend.model.*;
import com.passwordvault.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// NOTE: UserService is injected to ensure the user row exists in the `users` table
// before any child-table inserts (FK constraint enforcement).

/**
 * Core vault service.
 *
 * Encryption contract:
 *   - All encryption/decryption is done client-side.
 *   - Server stores only ciphertext (Base64 → byte[]).
 *   - Server never sees or logs plaintext passwords.
 *   - A new IV must be generated per version.
 *   - Max 20 versions per vault item enforced here.
 */
@Service
public class VaultService {

    private static final Logger logger = LoggerFactory.getLogger(VaultService.class);
    private static final int MAX_VERSIONS = 20;

    private final UserCryptoSettingsRepository cryptoSettingsRepository;
    private final VaultItemRepository vaultItemRepository;
    private final VaultItemVersionRepository versionRepository;
    private final VaultAuditLogRepository auditLogRepository;
    private final VaultViewSessionRepository viewSessionRepository;
    private final DeletedVaultItemRepository deletedVaultItemRepository;
    private final AuditService auditService;
    private final UserService userService;

    public VaultService(UserCryptoSettingsRepository cryptoSettingsRepository,
                        VaultItemRepository vaultItemRepository,
                        VaultItemVersionRepository versionRepository,
                        VaultAuditLogRepository auditLogRepository,
                        VaultViewSessionRepository viewSessionRepository,
                        DeletedVaultItemRepository deletedVaultItemRepository,
                        AuditService auditService,
                        UserService userService) {
        this.cryptoSettingsRepository = cryptoSettingsRepository;
        this.vaultItemRepository = vaultItemRepository;
        this.versionRepository = versionRepository;
        this.auditLogRepository = auditLogRepository;
        this.viewSessionRepository = viewSessionRepository;
        this.deletedVaultItemRepository = deletedVaultItemRepository;
        this.auditService = auditService;
        this.userService = userService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Crypto Settings
    // ─────────────────────────────────────────────────────────────────────────

    public Optional<CryptoSettingsResponse> getCryptoSettings(String userId) {
        return cryptoSettingsRepository.findById(userId)
                .map(this::toCryptoSettingsResponse);
    }

    @Transactional
    public CryptoSettingsResponse saveCryptoSettings(String userId, CryptoSettingsRequest req) {
        validateCryptoSettingsRequest(req);

        // Ensure user row exists in `users` table — required by FK constraint.
        // This is idempotent: if the user already exists it just updates lastSeenAt.
        ensureUserExists(userId);

        UserCryptoSettings settings = cryptoSettingsRepository.findById(userId)
                .orElse(new UserCryptoSettings());
        settings.setUserId(userId);
        settings.setKdfAlgorithm(req.getKdfAlgorithm().trim().toUpperCase());
        settings.setKdfSalt(Base64.getDecoder().decode(req.getKdfSalt()));
        settings.setKdfIterations(req.getKdfIterations());
        settings.setKdfMemoryKb(req.getKdfMemoryKb());
        settings.setKdfParallelism(req.getKdfParallelism());

        UserCryptoSettings saved = cryptoSettingsRepository.save(settings);
        logger.info("Saved crypto settings for userId={}", userId);
        return toCryptoSettingsResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Vault Items — CRUD
    // ─────────────────────────────────────────────────────────────────────────

    public List<VaultItemResponse> getVaultItems(String userId) {
        return vaultItemRepository
                .findByUserIdAndStatusOrderByUpdatedAtDesc(userId, "ACTIVE")
                .stream()
                .map(this::toVaultItemResponse)
                .collect(Collectors.toList());
    }

    public VaultItemResponse getVaultItem(String userId, Long vaultItemId,
                                          String ipAddress, String userAgent) {
        VaultItem item = requireOwnedActiveItem(userId, vaultItemId);
        auditService.logSuccess(userId, vaultItemId, "VIEW", ipAddress, userAgent);
        return toVaultItemResponse(item);
    }

    @Transactional
    public VaultItemResponse createVaultItem(String userId, CreateVaultItemRequest req,
                                             String ipAddress, String userAgent) {
        validateCreateRequest(req);

        // Ensure user row exists before child-table insert
        ensureUserExists(userId);

        // 1. Persist vault item
        VaultItem item = new VaultItem();
        item.setUserId(userId);
        item.setTitle(req.getTitle().trim());
        item.setWebsiteUrl(req.getWebsiteUrl());
        item.setUsername(req.getUsername());
        item.setNotesEncrypted(decodeBase64OrNull(req.getNotesEncrypted()));
        item.setStatus("ACTIVE");
        item.setCurrentVersion(1);
        VaultItem saved = vaultItemRepository.save(item);

        // 2. Persist the first version
        VaultItemVersion version = buildVersion(saved.getVaultItemId(), 1, req.getPasswordEncrypted(),
                req.getEncryptionIv(),
                req.getEncryptionAlgo() != null ? req.getEncryptionAlgo() : "AES-256-GCM",
                userId);
        versionRepository.save(version);

        // 3. Audit
        auditService.logSuccess(userId, saved.getVaultItemId(), "CREATE", ipAddress, userAgent);
        logger.info("Created vault item id={} for userId={}", saved.getVaultItemId(), userId);
        return toVaultItemResponse(saved);
    }

    @Transactional
    public VaultItemResponse updateVaultItem(String userId, Long vaultItemId,
                                             UpdateVaultItemRequest req,
                                             String ipAddress, String userAgent) {
        VaultItem item = requireOwnedActiveItem(userId, vaultItemId);

        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            item.setTitle(req.getTitle().trim());
        }
        if (req.getWebsiteUrl() != null) {
            item.setWebsiteUrl(req.getWebsiteUrl());
        }
        if (req.getUsername() != null) {
            item.setUsername(req.getUsername());
        }
        if (req.getNotesEncrypted() != null) {
            item.setNotesEncrypted(decodeBase64OrNull(req.getNotesEncrypted()));
        }

        VaultItem saved = vaultItemRepository.save(item);
        auditService.logSuccess(userId, vaultItemId, "UPDATE", ipAddress, userAgent);
        logger.info("Updated vault item id={} for userId={}", vaultItemId, userId);
        return toVaultItemResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Vault Item Versions
    // ─────────────────────────────────────────────────────────────────────────

    public List<VaultItemVersionSummaryResponse> getVersions(String userId, Long vaultItemId) {
        requireOwnedActiveItem(userId, vaultItemId);
        return versionRepository
                .findByVaultItemIdOrderByVersionNumberDesc(vaultItemId)
                .stream()
                .map(this::toVersionSummary)
                .collect(Collectors.toList());
    }

    public VaultItemVersionDetailResponse getVersion(String userId, Long vaultItemId,
                                                     int versionNumber,
                                                     String ipAddress, String userAgent) {
        requireOwnedActiveItem(userId, vaultItemId);
        VaultItemVersion version = versionRepository
                .findByVaultItemIdAndVersionNumber(vaultItemId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version " + versionNumber + " not found for vault item " + vaultItemId));

        // Audit and view session
        auditService.logSuccess(userId, vaultItemId, "VIEW_PASSWORD", ipAddress, userAgent);
        auditService.recordViewSession(userId, vaultItemId, ipAddress);

        return toVersionDetail(version);
    }

    @Transactional
    public VaultItemVersionDetailResponse addVersion(String userId, Long vaultItemId,
                                                     AddVersionRequest req,
                                                     String ipAddress, String userAgent) {
        validateAddVersionRequest(req);
        VaultItem item = requireOwnedActiveItem(userId, vaultItemId);

        // Enforce max 20 versions — delete oldest if at limit
        long count = versionRepository.countByVaultItemId(vaultItemId);
        if (count >= MAX_VERSIONS) {
            versionRepository.findTopByVaultItemIdOrderByVersionNumberAsc(vaultItemId)
                    .ifPresent(oldest -> {
                        logger.info("Purging oldest version {} for vaultItemId={}", oldest.getVersionNumber(), vaultItemId);
                        versionRepository.delete(oldest);
                    });
        }

        // Next version number = current max + 1
        int nextVersion = versionRepository
                .findTopByVaultItemIdOrderByVersionNumberDesc(vaultItemId)
                .map(v -> v.getVersionNumber() + 1)
                .orElse(2);

        VaultItemVersion version = buildVersion(vaultItemId, nextVersion,
                req.getPasswordEncrypted(), req.getEncryptionIv(),
                req.getEncryptionAlgo() != null ? req.getEncryptionAlgo() : "AES-256-GCM",
                userId);
        VaultItemVersion saved = versionRepository.save(version);

        // Update vault item's current version
        item.setCurrentVersion(nextVersion);
        vaultItemRepository.save(item);

        auditService.logSuccess(userId, vaultItemId, "ADD_VERSION", ipAddress, userAgent);
        logger.info("Added version {} for vaultItemId={}", nextVersion, vaultItemId);
        return toVersionDetail(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Soft Delete / Trash / Restore
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void softDeleteVaultItem(String userId, Long vaultItemId,
                                    String ipAddress, String userAgent) {
        VaultItem item = requireOwnedActiveItem(userId, vaultItemId);
        item.setStatus("DELETED");
        vaultItemRepository.save(item);

        DeletedVaultItem deleted = new DeletedVaultItem();
        deleted.setVaultItemId(vaultItemId);
        deleted.setUserId(userId);
        deletedVaultItemRepository.save(deleted);

        auditService.logSuccess(userId, vaultItemId, "DELETE", ipAddress, userAgent);
        logger.info("Soft-deleted vault item id={} for userId={}", vaultItemId, userId);
    }

    public List<DeletedVaultItemResponse> getTrash(String userId) {
        return deletedVaultItemRepository
                .findByUserIdOrderByDeletedAtDesc(userId)
                .stream()
                .map(d -> {
                    DeletedVaultItemResponse resp = new DeletedVaultItemResponse();
                    resp.setDeletedId(d.getDeletedId());
                    resp.setVaultItemId(d.getVaultItemId());
                    resp.setUserId(d.getUserId());
                    resp.setDeletedAt(d.getDeletedAt());
                    // Enrich with vault item metadata if still present
                    vaultItemRepository.findById(d.getVaultItemId()).ifPresent(item -> {
                        resp.setTitle(item.getTitle());
                        resp.setWebsiteUrl(item.getWebsiteUrl());
                        resp.setUsername(item.getUsername());
                        resp.setOriginalCreatedAt(item.getCreatedAt());
                    });
                    return resp;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public VaultItemResponse restoreVaultItem(String userId, Long vaultItemId,
                                              String ipAddress, String userAgent) {
        VaultItem item = vaultItemRepository.findByVaultItemIdAndUserId(vaultItemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vault item " + vaultItemId + " not found"));

        if (!"DELETED".equals(item.getStatus())) {
            throw new BadRequestException("Vault item is not in trash");
        }

        item.setStatus("ACTIVE");
        VaultItem saved = vaultItemRepository.save(item);

        deletedVaultItemRepository.findByVaultItemIdAndUserId(vaultItemId, userId)
                .ifPresent(deletedVaultItemRepository::delete);

        auditService.logSuccess(userId, vaultItemId, "RESTORE", ipAddress, userAgent);
        logger.info("Restored vault item id={} for userId={}", vaultItemId, userId);
        return toVaultItemResponse(saved);
    }

    @Transactional
    public void permanentlyDeleteVaultItem(String userId, Long vaultItemId,
                                           String ipAddress, String userAgent) {
        VaultItem item = vaultItemRepository.findByVaultItemIdAndUserId(vaultItemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vault item " + vaultItemId + " not found"));

        if (!"DELETED".equals(item.getStatus())) {
            throw new BadRequestException("Vault item must be soft-deleted (in trash) before permanent deletion");
        }

        deletedVaultItemRepository.deleteByVaultItemId(vaultItemId);
        vaultItemRepository.delete(item); // cascades to vault_item_versions

        auditService.logSuccess(userId, vaultItemId, "PERMANENT_DELETE", ipAddress, userAgent);
        logger.info("Permanently deleted vault item id={} for userId={}", vaultItemId, userId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Audit Logs
    // ─────────────────────────────────────────────────────────────────────────

    public List<AuditLogResponse> getAuditLogs(String userId) {
        return auditLogRepository
                .findByUserIdOrderByActionTimestampDesc(userId)
                .stream()
                .map(this::toAuditLogResponse)
                .collect(Collectors.toList());
    }

    public List<AuditLogResponse> getAuditLogsForItem(String userId, Long vaultItemId) {
        // Verify the item belongs to the user
        vaultItemRepository.findByVaultItemIdAndUserId(vaultItemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vault item " + vaultItemId + " not found"));
        return auditLogRepository
                .findByUserIdAndVaultItemIdOrderByActionTimestampDesc(userId, vaultItemId)
                .stream()
                .map(this::toAuditLogResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // View Sessions
    // ─────────────────────────────────────────────────────────────────────────

    public List<ViewSessionResponse> getViewSessions(String userId) {
        return viewSessionRepository
                .findByUserIdOrderByViewedAtDesc(userId)
                .stream()
                .map(this::toViewSessionResponse)
                .collect(Collectors.toList());
    }

    public List<ViewSessionResponse> getViewSessionsForItem(String userId, Long vaultItemId) {
        vaultItemRepository.findByVaultItemIdAndUserId(vaultItemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vault item " + vaultItemId + " not found"));
        return viewSessionRepository
                .findByUserIdAndVaultItemIdOrderByViewedAtDesc(userId, vaultItemId)
                .stream()
                .map(this::toViewSessionResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private VaultItem requireOwnedActiveItem(String userId, Long vaultItemId) {
        VaultItem item = vaultItemRepository.findByVaultItemIdAndUserId(vaultItemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vault item " + vaultItemId + " not found"));
        if (!"ACTIVE".equals(item.getStatus())) {
            throw new ResourceNotFoundException("Vault item " + vaultItemId + " is not active");
        }
        return item;
    }

    private VaultItemVersion buildVersion(Long vaultItemId, int versionNumber,
                                          String passwordEncryptedBase64, String encryptionIvBase64,
                                          String encryptionAlgo, String createdBy) {
        VaultItemVersion v = new VaultItemVersion();
        v.setVaultItemId(vaultItemId);
        v.setVersionNumber(versionNumber);
        v.setPasswordEncrypted(Base64.getDecoder().decode(passwordEncryptedBase64));
        v.setEncryptionIv(Base64.getDecoder().decode(encryptionIvBase64));
        v.setEncryptionAlgo(encryptionAlgo);
        v.setCreatedBy(createdBy);
        return v;
    }

    private byte[] decodeBase64OrNull(String base64) {
        if (base64 == null || base64.isBlank()) return null;
        return Base64.getDecoder().decode(base64);
    }

    private String encodeBase64OrNull(byte[] data) {
        if (data == null) return null;
        return Base64.getEncoder().encodeToString(data);
    }

    // ─ Mappers ─

    private CryptoSettingsResponse toCryptoSettingsResponse(UserCryptoSettings s) {
        CryptoSettingsResponse r = new CryptoSettingsResponse();
        r.setUserId(s.getUserId());
        r.setKdfAlgorithm(s.getKdfAlgorithm());
        r.setKdfSalt(Base64.getEncoder().encodeToString(s.getKdfSalt()));
        r.setKdfIterations(s.getKdfIterations());
        r.setKdfMemoryKb(s.getKdfMemoryKb());
        r.setKdfParallelism(s.getKdfParallelism());
        r.setCreatedAt(s.getCreatedAt());
        r.setUpdatedAt(s.getUpdatedAt());
        return r;
    }

    private VaultItemResponse toVaultItemResponse(VaultItem item) {
        VaultItemResponse r = new VaultItemResponse();
        r.setVaultItemId(item.getVaultItemId());
        r.setUserId(item.getUserId());
        r.setTitle(item.getTitle());
        r.setWebsiteUrl(item.getWebsiteUrl());
        r.setUsername(item.getUsername());
        r.setNotesEncrypted(encodeBase64OrNull(item.getNotesEncrypted()));
        r.setStatus(item.getStatus());
        r.setCurrentVersion(item.getCurrentVersion());
        r.setCreatedAt(item.getCreatedAt());
        r.setUpdatedAt(item.getUpdatedAt());
        return r;
    }

    private VaultItemVersionSummaryResponse toVersionSummary(VaultItemVersion v) {
        VaultItemVersionSummaryResponse r = new VaultItemVersionSummaryResponse();
        r.setVersionId(v.getVersionId());
        r.setVaultItemId(v.getVaultItemId());
        r.setVersionNumber(v.getVersionNumber());
        r.setEncryptionAlgo(v.getEncryptionAlgo());
        r.setCreatedBy(v.getCreatedBy());
        r.setCreatedAt(v.getCreatedAt());
        return r;
    }

    private VaultItemVersionDetailResponse toVersionDetail(VaultItemVersion v) {
        VaultItemVersionDetailResponse r = new VaultItemVersionDetailResponse();
        r.setVersionId(v.getVersionId());
        r.setVaultItemId(v.getVaultItemId());
        r.setVersionNumber(v.getVersionNumber());
        r.setPasswordEncrypted(Base64.getEncoder().encodeToString(v.getPasswordEncrypted()));
        r.setEncryptionIv(Base64.getEncoder().encodeToString(v.getEncryptionIv()));
        r.setEncryptionAlgo(v.getEncryptionAlgo());
        r.setCreatedBy(v.getCreatedBy());
        r.setCreatedAt(v.getCreatedAt());
        return r;
    }

    private AuditLogResponse toAuditLogResponse(VaultAuditLog a) {
        AuditLogResponse r = new AuditLogResponse();
        r.setAuditId(a.getAuditId());
        r.setUserId(a.getUserId());
        r.setVaultItemId(a.getVaultItemId());
        r.setActionType(a.getActionType());
        r.setIpAddress(a.getIpAddress());
        r.setUserAgent(a.getUserAgent());
        r.setStatus(a.getStatus());
        r.setActionTimestamp(a.getActionTimestamp());
        return r;
    }

    private ViewSessionResponse toViewSessionResponse(VaultViewSession s) {
        ViewSessionResponse r = new ViewSessionResponse();
        r.setSessionId(s.getSessionId());
        r.setUserId(s.getUserId());
        r.setVaultItemId(s.getVaultItemId());
        r.setSessionIp(s.getSessionIp());
        r.setViewedAt(s.getViewedAt());
        return r;
    }

    // ─ Validation ─

    private void validateCryptoSettingsRequest(CryptoSettingsRequest req) {
        if (req.getKdfAlgorithm() == null || req.getKdfAlgorithm().isBlank()) {
            throw new BadRequestException("kdfAlgorithm is required");
        }
        if (req.getKdfSalt() == null || req.getKdfSalt().isBlank()) {
            throw new BadRequestException("kdfSalt (Base64) is required");
        }
        if (req.getKdfIterations() <= 0) {
            throw new BadRequestException("kdfIterations must be positive");
        }
        try {
            Base64.getDecoder().decode(req.getKdfSalt());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("kdfSalt must be valid Base64");
        }
    }

    private void validateCreateRequest(CreateVaultItemRequest req) {
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new BadRequestException("title is required");
        }
        if (req.getPasswordEncrypted() == null || req.getPasswordEncrypted().isBlank()) {
            throw new BadRequestException("passwordEncrypted (Base64) is required");
        }
        if (req.getEncryptionIv() == null || req.getEncryptionIv().isBlank()) {
            throw new BadRequestException("encryptionIv (Base64) is required");
        }
        tryDecodeBase64("passwordEncrypted", req.getPasswordEncrypted());
        tryDecodeBase64("encryptionIv", req.getEncryptionIv());
        if (req.getNotesEncrypted() != null && !req.getNotesEncrypted().isBlank()) {
            tryDecodeBase64("notesEncrypted", req.getNotesEncrypted());
        }
    }

    private void validateAddVersionRequest(AddVersionRequest req) {
        if (req.getPasswordEncrypted() == null || req.getPasswordEncrypted().isBlank()) {
            throw new BadRequestException("passwordEncrypted (Base64) is required");
        }
        if (req.getEncryptionIv() == null || req.getEncryptionIv().isBlank()) {
            throw new BadRequestException("encryptionIv (Base64) is required");
        }
        tryDecodeBase64("passwordEncrypted", req.getPasswordEncrypted());
        tryDecodeBase64("encryptionIv", req.getEncryptionIv());
    }

    private void tryDecodeBase64(String fieldName, String value) {
        try {
            Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(fieldName + " must be valid Base64");
        }
    }

    /**
     * Upserts the user row in the `users` table so that FK constraints on child
     * tables (user_crypto_settings, vault_items, …) are always satisfied.
     * Safe to call repeatedly — if the user already exists it only updates lastSeenAt.
     */
    private void ensureUserExists(String userId) {
        User u = new User();
        u.setUserId(userId);
        userService.createOrUpdateUser(u);
    }
}

