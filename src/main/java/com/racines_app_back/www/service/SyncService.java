package com.racines_app_back.www.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.racines_app_back.www.domain.dto.SyncOperationCreateDTO;
import com.racines_app_back.www.domain.dto.SyncOperationDTO;
import com.racines_app_back.www.domain.entity.SyncQueue;
import com.racines_app_back.www.domain.enums.EntityType;
import com.racines_app_back.www.domain.enums.OperationType;
import com.racines_app_back.www.domain.enums.SyncStatus;
import com.racines_app_back.www.domain.repository.SyncQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SyncService {

    private final SyncQueueRepository syncQueueRepository;
    private final ObjectMapper objectMapper;
    private static final int MAX_RETRIES = 3;

    public SyncOperationDTO queueOperation(SyncOperationCreateDTO dto, UUID userId) {
        String payloadJson = null;
        if (dto.getPayload() != null) {
            try {
                payloadJson = objectMapper.writeValueAsString(dto.getPayload());
            } catch (JsonProcessingException e) {
                log.error("Erreur lors de la sérialisation du payload: {}", e.getMessage());
                throw new RuntimeException("Erreur lors de la sérialisation du payload", e);
            }
        }

        SyncQueue syncQueue = SyncQueue.builder()
                .userId(userId)
                .operationType(dto.getOperationType())
                .entityType(dto.getEntityType())
                .entityId(dto.getEntityId())
                .payload(payloadJson)
                .status(SyncStatus.PENDING)
                .retryCount(0)
                .build();

        SyncQueue saved = syncQueueRepository.save(syncQueue);
        log.info("Opération ajoutée à la queue de synchronisation: {}", saved.getId());
        
        return toDTO(saved);
    }

    public List<SyncOperationDTO> syncPendingOperations(UUID userId) {
        List<SyncQueue> pendingOperations = syncQueueRepository
                .findPendingByUserIdOrderByCreatedAt(userId);

        return pendingOperations.stream()
                .map(this::processSyncOperation)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private SyncQueue processSyncOperation(SyncQueue syncQueue) {
        try {
            // Ici, on devrait appeler les services appropriés selon le type d'entité
            // Pour l'instant, on simule le succès
            // Dans une implémentation complète, on devrait:
            // - Parser le payload JSON
            // - Appeler le service approprié (PersonService, etc.)
            // - Gérer les erreurs

            syncQueue.setStatus(SyncStatus.SYNCED);
            syncQueue.setSyncedAt(LocalDateTime.now());
            
            log.info("Opération synchronisée avec succès: {}", syncQueue.getId());
        } catch (Exception e) {
            log.error("Erreur lors de la synchronisation de l'opération {}: {}", 
                    syncQueue.getId(), e.getMessage());
            
            syncQueue.setRetryCount(syncQueue.getRetryCount() + 1);
            syncQueue.setErrorMessage(e.getMessage());
            
            if (syncQueue.getRetryCount() >= MAX_RETRIES) {
                syncQueue.setStatus(SyncStatus.FAILED);
                log.error("Opération marquée comme échouée après {} tentatives: {}", 
                        MAX_RETRIES, syncQueue.getId());
            }
        }

        return syncQueueRepository.save(syncQueue);
    }

    public List<SyncOperationDTO> retryFailedOperations(UUID userId) {
        List<SyncQueue> failedOperations = syncQueueRepository
                .findFailedWithRetryLimit(MAX_RETRIES);

        return failedOperations.stream()
                .filter(op -> op.getUserId().equals(userId))
                .map(op -> {
                    op.setStatus(SyncStatus.PENDING);
                    op.setErrorMessage(null);
                    return syncQueueRepository.save(op);
                })
                .map(this::processSyncOperation)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<SyncOperationDTO> getPendingOperations(UUID userId) {
        return syncQueueRepository.findPendingByUserIdOrderByCreatedAt(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public long getPendingOperationsCount(UUID userId) {
        return syncQueueRepository.countPendingByUserId(userId);
    }

    private SyncOperationDTO toDTO(SyncQueue syncQueue) {
        return SyncOperationDTO.builder()
                .id(syncQueue.getId())
                .userId(syncQueue.getUserId())
                .operationType(syncQueue.getOperationType())
                .entityType(syncQueue.getEntityType())
                .entityId(syncQueue.getEntityId())
                .payload(syncQueue.getPayload())
                .status(syncQueue.getStatus())
                .errorMessage(syncQueue.getErrorMessage())
                .retryCount(syncQueue.getRetryCount())
                .createdAt(syncQueue.getCreatedAt())
                .syncedAt(syncQueue.getSyncedAt())
                .build();
    }
}
