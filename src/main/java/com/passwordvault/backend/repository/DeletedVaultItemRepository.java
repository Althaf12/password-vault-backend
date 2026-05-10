package com.passwordvault.backend.repository;

import com.passwordvault.backend.model.DeletedVaultItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeletedVaultItemRepository extends JpaRepository<DeletedVaultItem, Long> {

    List<DeletedVaultItem> findByUserIdOrderByDeletedAtDesc(String userId);

    Optional<DeletedVaultItem> findByVaultItemIdAndUserId(Long vaultItemId, String userId);

    void deleteByVaultItemId(Long vaultItemId);
}

