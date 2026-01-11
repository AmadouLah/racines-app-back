package com.racines_app_back.www.domain.dto;

import com.racines_app_back.www.domain.enums.EntityType;
import com.racines_app_back.www.domain.enums.OperationType;
import com.racines_app_back.www.domain.enums.SyncStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncOperationDTO {
    private UUID id;
    private UUID userId;
    private OperationType operationType;
    private EntityType entityType;
    private UUID entityId;
    private String payload;
    private SyncStatus status;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime syncedAt;
}
