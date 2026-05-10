package com.passwordvault.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for adding a new encrypted password version to an existing vault item.
 * All binary data must be Base64-encoded. A new IV must be generated per version.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddVersionRequest {
    /** Base64-encoded encrypted password (required) */
    private String passwordEncrypted;
    /** Base64-encoded IV unique to this version (required) */
    private String encryptionIv;
    /** Encryption algorithm — defaults to AES-256-GCM */
    private String encryptionAlgo;
}

