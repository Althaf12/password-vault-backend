package com.passwordvault.backend.exception;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(String userId) {
        super("User", "userId", userId);
    }
}

