package com.passwordvault.backend.repository;

import com.passwordvault.backend.model.VaultViewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VaultViewSessionRepository extends JpaRepository<VaultViewSession, Long> {

    List<VaultViewSession> findByUserIdOrderByViewedAtDesc(String userId);

    List<VaultViewSession> findByUserIdAndVaultItemIdOrderByViewedAtDesc(String userId, Long vaultItemId);
}

