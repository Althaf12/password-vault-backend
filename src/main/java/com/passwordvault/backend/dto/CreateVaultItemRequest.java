package com.passwordvault.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new vault item.
 * The first password version is included in this request.
 * All binary data (notesEncrypted, passwordEncrypted, encryptionIv) must be Base64-encoded.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateVaultItemRequest {
    /** Display name of the vault entry (e.g. "Gmail") */
    private String title;
    /** Optional website URL (stored in plaintext) */
    private String websiteUrl;
    /** Optional username / email (stored in plaintext) */
    private String username;
    /** Optional Base64-encoded AES-256-GCM encrypted notes */
    private String notesEncrypted;
    /** Base64-encoded encrypted password for the initial version (required) */
    private String passwordEncrypted;
    /** Base64-encoded IV used to encrypt passwordEncrypted (required) */
    private String encryptionIv;
    /** Encryption algorithm — defaults to AES-256-GCM */
    private String encryptionAlgo;
}

