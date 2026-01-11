package com.racines_app_back.www.controller;

import com.racines_app_back.www.domain.dto.ApiResponse;
import com.racines_app_back.www.domain.dto.SyncOperationCreateDTO;
import com.racines_app_back.www.domain.dto.SyncOperationDTO;
import com.racines_app_back.www.service.CurrentUserService;
import com.racines_app_back.www.service.SyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;
    private final CurrentUserService currentUserService;

    @PostMapping("/queue")
    public ResponseEntity<ApiResponse<SyncOperationDTO>> queueOperation(
            @Valid @RequestBody SyncOperationCreateDTO dto) {
        UUID userId = currentUserService.getCurrentUserId();
        SyncOperationDTO operation = syncService.queueOperation(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Opération ajoutée à la queue", operation));
    }

    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<List<SyncOperationDTO>>> executeSync() {
        UUID userId = currentUserService.getCurrentUserId();
        List<SyncOperationDTO> operations = syncService.syncPendingOperations(userId);
        return ResponseEntity.ok(ApiResponse.success("Synchronisation effectuée", operations));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSyncStatus() {
        UUID userId = currentUserService.getCurrentUserId();
        long pendingCount = syncService.getPendingOperationsCount(userId);
        List<SyncOperationDTO> pendingOperations = syncService.getPendingOperations(userId);
        
        Map<String, Object> status = new HashMap<>();
        status.put("pendingCount", pendingCount);
        status.put("pendingOperations", pendingOperations);
        
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @PostMapping("/retry")
    public ResponseEntity<ApiResponse<List<SyncOperationDTO>>> retryFailedOperations() {
        UUID userId = currentUserService.getCurrentUserId();
        List<SyncOperationDTO> operations = syncService.retryFailedOperations(userId);
        return ResponseEntity.ok(ApiResponse.success("Nouvelle tentative de synchronisation effectuée", operations));
    }
}
