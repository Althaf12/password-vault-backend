package com.passwordvault.backend.controller;

import com.passwordvault.backend.dto.UserRequest;
import com.passwordvault.backend.dto.UserResponse;
import com.passwordvault.backend.exception.BadRequestException;
import com.passwordvault.backend.exception.UserNotFoundException;
import com.passwordvault.backend.model.User;
import com.passwordvault.backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Create or update a user. Called by the frontend after successful authentication
     * with the Auth service. The userId comes from the JWT token claims.
     */
    @PostMapping("")
    public ResponseEntity<?> createOrUpdateUser(@RequestBody UserRequest request) {
        if (request == null || request.getUserId() == null || request.getUserId().isBlank()) {
            logger.warn("createOrUpdateUser called with null or empty userId");
            throw new BadRequestException("userId is required");
        }
        String userId = request.getUserId().trim();
        logger.info("createOrUpdateUser called for userId={}", userId);

        try {
            boolean isNewUser = userService.findById(userId).isEmpty();

            User u = new User();
            u.setUserId(userId);
            User saved = userService.createOrUpdateUser(u);

            if (isNewUser) {
                logger.info("New user created: {}. Add any app-specific initialization here.", userId);
                // TODO: Add any password-vault-specific initialization for new users here
                // e.g., create default vault, seed initial data, etc.
            }

            logger.info("User created/updated: userId={}", saved.getUserId());
            return ResponseEntity.ok(Map.of("status", "success", "userId", saved.getUserId()));
        } catch (IllegalArgumentException ex) {
            logger.warn("Validation error during createOrUpdateUser: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * Logout — updates lastSeenAt timestamp for the user.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        if (userId == null || userId.isBlank()) {
            logger.warn("logout called without userId");
            throw new BadRequestException("userId is required");
        }
        userService.updateLastSeenAt(userId.trim());
        logger.info("User logged out: {}", userId);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    /**
     * Delete a user and all their related data.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable String userId) {
        if (userId == null || userId.isBlank()) {
            logger.warn("deleteUser called with empty userId");
            throw new BadRequestException("userId is required");
        }
        var optUser = userService.findById(userId);
        if (optUser.isEmpty()) {
            logger.warn("deleteUser called for non-existing userId={}", userId);
            throw new UserNotFoundException(userId);
        }
        // TODO: Delete all app-specific data for this user before deleting the user
        userService.deleteUser(userId);
        logger.info("Deleted user for userId={}", userId);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    /**
     * Get user details by userId.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserDetails(@PathVariable String userId) {
        if (userId == null || userId.isBlank()) {
            logger.warn("getUserDetails called without userId");
            throw new BadRequestException("userId is required");
        }
        var opt = userService.findById(userId.trim());
        if (opt.isEmpty()) {
            logger.warn("getUserDetails: user not found userId={}", userId);
            throw new UserNotFoundException(userId);
        }
        User user = opt.get();
        UserResponse resp = new UserResponse();
        resp.setUserId(user.getUserId());
        resp.setStatus(user.getStatus());
        resp.setLastSeenAt(user.getLastSeenAt());
        resp.setCreatedAt(user.getCreatedAt());
        logger.info("getUserDetails successful for userId={}", user.getUserId());
        return ResponseEntity.ok(resp);
    }
}
