package com.passwordvault.backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // Register all caches used by the application so @Cacheable/@CacheEvict can find them
        // Add your new app's cache names here alongside "users"
        return new ConcurrentMapCacheManager("users");
    }
}

