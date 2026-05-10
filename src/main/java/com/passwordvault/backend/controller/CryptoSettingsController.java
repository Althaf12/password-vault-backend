package com.passwordvault.backend.controller;

import com.passwordvault.backend.dto.CryptoSettingsRequest;
import com.passwordvault.backend.dto.CryptoSettingsResponse;
import com.passwordvault.backend.exception.BadRequestException;
import com.passwordvault.backend.security.AuthenticatedUserService;
import com.passwordvault.backend.service.VaultService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Manages KDF (Key Derivation Function) crypto settings per user.
 *
 * GET  /api/vault/crypto-settings          → get current user's KDF parameters
 * PUT  /api/vault/crypto-settings          → create or update KDF parameters
 */
@RestController
@RequestMapping("/api/vault/crypto-settings")
public class CryptoSettingsController {

    private static final Logger logger = LoggerFactory.getLogger(CryptoSettingsController.class);

    private final VaultService vaultService;
    private final AuthenticatedUserService authenticatedUserService;

    public CryptoSettingsController(VaultService vaultService,
                                    AuthenticatedUserService authenticatedUserService) {
        this.vaultService = vaultService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping
    public ResponseEntity<?> getCryptoSettings() {
        String userId = authenticatedUserService.getCurrentUserId();
        return vaultService.getCryptoSettings(userId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<?> saveCryptoSettings(@RequestBody CryptoSettingsRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        String userId = authenticatedUserService.getCurrentUserId();
        logger.info("saveCryptoSettings for userId={}", userId);
        CryptoSettingsResponse response = vaultService.saveCryptoSettings(userId, request);
        return ResponseEntity.ok(response);
    }
}

