package com.passwordvault.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Full vault item response including metadata.
 * notesEncrypted is returned as a Base64 string when present.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VaultItemResponse {
    private Long vaultItemId;
    private String userId;
    private String title;
    private String websiteUrl;
    private String username;
    /** Base64-encoded encrypted notes, or null */
    private String notesEncrypted;
    private String status;
    private int currentVersion;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}

