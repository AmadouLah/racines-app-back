package com.racines_app_back.www.domain.entity;

import com.racines_app_back.www.domain.enums.EntityType;
import com.racines_app_back.www.domain.enums.OperationType;
import com.racines_app_back.www.domain.enums.SyncStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sync_queue", indexes = {
    @Index(name = "idx_sync_user_id", columnList = "user_id"),
    @Index(name = "idx_sync_status", columnList = "status"),
    @Index(name = "idx_sync_entity_type", columnList = "entity_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private OperationType operationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private EntityType entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SyncStatus status = SyncStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;
}
