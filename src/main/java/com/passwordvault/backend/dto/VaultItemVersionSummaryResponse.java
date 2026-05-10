package com.passwordvault.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Summary of a vault item version — does NOT include the encrypted password.
 * Used for listing versions safely.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VaultItemVersionSummaryResponse {
    private Long versionId;
    private Long vaultItemId;
    private int versionNumber;
    private String encryptionAlgo;
    private String createdBy;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}

