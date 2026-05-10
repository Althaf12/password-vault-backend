package com.passwordvault.backend.controller;

import com.passwordvault.backend.dto.*;
import com.passwordvault.backend.exception.BadRequestException;
import com.passwordvault.backend.security.AuthenticatedUserService;
import com.passwordvault.backend.service.VaultService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Main vault REST controller.
 *
 * ─ Vault Items ─────────────────────────────────────────────────────────────
 * GET    /api/vault/items                              - list all active items
 * POST   /api/vault/items                              - create item (incl. first password version)
 * GET    /api/vault/items/{id}                         - get item metadata (logs VIEW)
 * PUT    /api/vault/items/{id}                         - update metadata only
 * DELETE /api/vault/items/{id}                         - soft-delete (move to trash)
 *
 * ─ Versions ────────────────────────────────────────────────────────────────
 * GET    /api/vault/items/{id}/versions                - list version summaries (no ciphertext)
 * POST   /api/vault/items/{id}/versions                - add new encrypted password version
 * GET    /api/vault/items/{id}/versions/{versionNo}    - get encrypted password (logs VIEW_PASSWORD)
 *
 * ─ Trash ───────────────────────────────────────────────────────────────────
 * GET    /api/vault/trash                              - list trash
 * POST   /api/vault/trash/{id}/restore                 - restore from trash
 * DELETE /api/vault/trash/{id}                         - permanently delete
 *
 * ─ Audit ───────────────────────────────────────────────────────────────────
 * GET    /api/vault/audit                              - all audit events for user
 * GET    /api/vault/audit/{id}                         - audit events for a specific item
 *
 * ─ View Sessions ───────────────────────────────────────────────────────────
 * GET    /api/vault/view-sessions                      - all view sessions for user
 * GET    /api/vault/view-sessions/{id}                 - view sessions for a specific item
 */
@RestController
@RequestMapping("/api/vault")
public class VaultController {

    private static final Logger logger = LoggerFactory.getLogger(VaultController.class);

    private final VaultService vaultService;
    private final AuthenticatedUserService authenticatedUserService;

    public VaultController(VaultService vaultService,
                           AuthenticatedUserService authenticatedUserService) {
        this.vaultService = vaultService;
        this.authenticatedUserService = authenticatedUserService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Vault Items
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/items")
    public ResponseEntity<List<VaultItemResponse>> listVaultItems() {
        String userId = authenticatedUserService.getCurrentUserId();
        return ResponseEntity.ok(vaultService.getVaultItems(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<VaultItemResponse> createVaultItem(
            @RequestBody CreateVaultItemRequest request,
            HttpServletRequest httpRequest) {
        if (request == null) throw new BadRequestException("Request body is required");
        String userId = authenticatedUserService.getCurrentUserId();
        logger.info("createVaultItem for userId={}", userId);
        VaultItemResponse response = vaultService.createVaultItem(
                userId, request, getIp(httpRequest), getAgent(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/items/{vaultItemId}")
    public ResponseEntity<VaultItemResponse> getVaultItem(
            @PathVariable Long vaultItemId,
            HttpServletRequest httpRequest) {
        String userId = authenticatedUserService.getCurrentUserId();
        VaultItemResponse response = vaultService.getVaultItem(
                userId, vaultItemId, getIp(httpRequest), getAgent(httpRequest));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/items/{vaultItemId}")
    public ResponseEntity<VaultItemResponse> updateVaultItem(
            @PathVariable Long vaultItemId,
            @RequestBody UpdateVaultItemRequest request,
            HttpServletRequest httpRequest) {
        if (request == null) throw new BadRequestException("Request body is required");
        String userId = authenticatedUserService.getCurrentUserId();
        logger.info("updateVaultItem id={} for userId={}", vaultItemId, userId);
        VaultItemResponse response = vaultService.updateVaultItem(
                userId, vaultItemId, request, getIp(httpRequest), getAgent(httpRequest));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{vaultItemId}")
    public ResponseEntity<?> softDeleteVaultItem(
            @PathVariable Long vaultItemId,
            HttpServletRequest httpRequest) {
        String userId = authenticatedUserService.getCurrentUserId();
        logger.info("softDeleteVaultItem id={} for userId={}", vaultItemId, userId);
        vaultService.softDeleteVaultItem(userId, vaultItemId, getIp(httpRequest), getAgent(httpRequest));
        return ResponseEntity.ok(Map.of("status", "success", "message", "Item moved to trash"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Versions
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/items/{vaultItemId}/versions")
    public ResponseEntity<List<VaultItemVersionSummaryResponse>> listVersions(
            @PathVariable Long vaultItemId) {
        String userId = authenticatedUserService.getCurrentUserId();
        return ResponseEntity.ok(vaultService.getVersions(userId, vaultItemId));
    }

    @PostMapping("/items/{vaultItemId}/versions")
    public ResponseEntity<VaultItemVersionDetailResponse> addVersion(
            @PathVariable Long vaultItemId,
            @RequestBody AddVersionRequest request,
            HttpServletRequest httpRequest) {
        if (request == null) throw new BadRequestException("Request body is required");
        String userId = authenticatedUserService.getCurrentUserId();
        logger.info("addVersion for vaultItemId={} userId={}", vaultItemId, userId);
        VaultItemVersionDetailResponse response = vaultService.addVersion(
                userId, vaultItemId, request, getIp(httpRequest), getAgent(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/items/{vaultItemId}/versions/{versionNumber}")
    public ResponseEntity<VaultItemVersionDetailResponse> getVersion(
            @PathVariable Long vaultItemId,
            @PathVariable int versionNumber,
            HttpServletRequest httpRequest) {
        String userId = authenticatedUserService.getCurrentUserId();
        VaultItemVersionDetailResponse response = vaultService.getVersion(
                userId, vaultItemId, versionNumber, getIp(httpRequest), getAgent(httpRequest));
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Trash
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/trash")
    public ResponseEntity<List<DeletedVaultItemResponse>> getTrash() {
        String userId = authenticatedUserService.getCurrentUserId();
        return ResponseEntity.ok(vaultService.getTrash(userId));
    }

    @PostMapping("/trash/{vaultItemId}/restore")
    public ResponseEntity<VaultItemResponse> restoreVaultItem(
            @PathVariable Long vaultItemId,
            HttpServletRequest httpRequest) {
        String userId = authenticatedUserService.getCurrentUserId();
        logger.info("restoreVaultItem id={} for userId={}", vaultItemId, userId);
        VaultItemResponse response = vaultService.restoreVaultItem(
                userId, vaultItemId, getIp(httpRequest), getAgent(httpRequest));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/trash/{vaultItemId}")
    public ResponseEntity<?> permanentlyDeleteVaultItem(
            @PathVariable Long vaultItemId,
            HttpServletRequest httpRequest) {
        String userId = authenticatedUserService.getCurrentUserId();
        logger.info("permanentlyDeleteVaultItem id={} for userId={}", vaultItemId, userId);
        vaultService.permanentlyDeleteVaultItem(userId, vaultItemId, getIp(httpRequest), getAgent(httpRequest));
        return ResponseEntity.ok(Map.of("status", "success", "message", "Item permanently deleted"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Audit Logs
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/audit")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs() {
        String userId = authenticatedUserService.getCurrentUserId();
        return ResponseEntity.ok(vaultService.getAuditLogs(userId));
    }

    @GetMapping("/audit/{vaultItemId}")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogsForItem(
            @PathVariable Long vaultItemId) {
        String userId = authenticatedUserService.getCurrentUserId();
        return ResponseEntity.ok(vaultService.getAuditLogsForItem(userId, vaultItemId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // View Sessions
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/view-sessions")
    public ResponseEntity<List<ViewSessionResponse>> getViewSessions() {
        String userId = authenticatedUserService.getCurrentUserId();
        return ResponseEntity.ok(vaultService.getViewSessions(userId));
    }

    @GetMapping("/view-sessions/{vaultItemId}")
    public ResponseEntity<List<ViewSessionResponse>> getViewSessionsForItem(
            @PathVariable Long vaultItemId) {
        String userId = authenticatedUserService.getCurrentUserId();
        return ResponseEntity.ok(vaultService.getViewSessionsForItem(userId, vaultItemId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String getIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private String getAgent(HttpServletRequest req) {
        String agent = req.getHeader("User-Agent");
        return agent != null && agent.length() > 500 ? agent.substring(0, 500) : agent;
    }
}

