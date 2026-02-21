package com.passwordvault.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PasswordVaultBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PasswordVaultBackendApplication.class, args);
    }
}
