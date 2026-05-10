package com.passwordvault.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for saving/updating KDF (Key Derivation Function) settings.
 * Binary fields (kdfSalt) are transmitted as Base64-encoded strings.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CryptoSettingsRequest {
    /** KDF algorithm: Argon2id or PBKDF2 */
    private String kdfAlgorithm;
    /** Base64-encoded random salt used for key derivation */
    private String kdfSalt;
    /** Number of iterations (PBKDF2) or time cost (Argon2id) */
    private int kdfIterations;
    /** Memory cost in KB — required for Argon2id */
    private Integer kdfMemoryKb;
    /** Parallelism — required for Argon2id */
    private Integer kdfParallelism;
}

