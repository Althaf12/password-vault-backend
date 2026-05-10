package com.passwordvault.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating vault item metadata (title, url, username, notes).
 * Does NOT update the password — use AddVersionRequest for that.
 * notesEncrypted must be Base64-encoded if provided.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVaultItemRequest {
    private String title;
    private String websiteUrl;
    private String username;
    /** Optional Base64-encoded AES-256-GCM encrypted notes */
    private String notesEncrypted;
}

