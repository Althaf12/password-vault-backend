package com.passwordvault.backend.repository;

import com.passwordvault.backend.model.UserCryptoSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCryptoSettingsRepository extends JpaRepository<UserCryptoSettings, String> {
}

