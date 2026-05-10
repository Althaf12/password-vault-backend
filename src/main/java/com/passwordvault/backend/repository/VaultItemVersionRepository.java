package com.passwordvault.backend.repository;

import com.passwordvault.backend.model.VaultItemVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VaultItemVersionRepository extends JpaRepository<VaultItemVersion, Long> {

    List<VaultItemVersion> findByVaultItemIdOrderByVersionNumberDesc(Long vaultItemId);

    Optional<VaultItemVersion> findByVaultItemIdAndVersionNumber(Long vaultItemId, int versionNumber);

    long countByVaultItemId(Long vaultItemId);

    Optional<VaultItemVersion> findTopByVaultItemIdOrderByVersionNumberAsc(Long vaultItemId);

    Optional<VaultItemVersion> findTopByVaultItemIdOrderByVersionNumberDesc(Long vaultItemId);
}

