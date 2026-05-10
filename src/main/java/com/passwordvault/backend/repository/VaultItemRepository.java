package com.passwordvault.backend.repository;

import com.passwordvault.backend.model.VaultItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VaultItemRepository extends JpaRepository<VaultItem, Long> {

    List<VaultItem> findByUserIdAndStatusOrderByUpdatedAtDesc(String userId, String status);

    Optional<VaultItem> findByVaultItemIdAndUserId(Long vaultItemId, String userId);
}

