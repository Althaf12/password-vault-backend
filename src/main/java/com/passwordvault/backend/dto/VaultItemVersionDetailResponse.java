package com.passwordvault.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Full version detail — includes the encrypted password and IV.
 * Only returned when a specific version is explicitly requested (logs VIEW_PASSWORD audit event).
 * passwordEncrypted and encryptionIv are Base64-encoded.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VaultItemVersionDetailResponse {
    private Long versionId;
    private Long vaultItemId;
    private int versionNumber;
    /** Base64-encoded encrypted password */
    private String passwordEncrypted;
    /** Base64-encoded IV */
    private String encryptionIv;
    private String encryptionAlgo;
    private String createdBy;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}

