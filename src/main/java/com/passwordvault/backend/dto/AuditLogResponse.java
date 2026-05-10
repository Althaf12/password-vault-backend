package com.passwordvault.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long auditId;
    private String userId;
    private Long vaultItemId;
    /** Action types: CREATE, VIEW, VIEW_PASSWORD, UPDATE, ADD_VERSION, DELETE, RESTORE, PERMANENT_DELETE */
    private String actionType;
    private String ipAddress;
    private String userAgent;
    private String status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime actionTimestamp;
}

