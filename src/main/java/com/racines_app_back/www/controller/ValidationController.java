package com.racines_app_back.www.controller;

import com.racines_app_back.www.domain.dto.ApiResponse;
import com.racines_app_back.www.domain.dto.PendingAdditionDTO;
import com.racines_app_back.www.domain.dto.ValidationRequestDTO;
import com.racines_app_back.www.service.CurrentUserService;
import com.racines_app_back.www.service.ValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/validations")
@RequiredArgsConstructor
public class ValidationController {

    private final ValidationService validationService;
    private final CurrentUserService currentUserService;

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<PendingAdditionDTO>> requestValidation(
            @Valid @RequestBody ValidationRequestDTO dto) {
        UUID userId = currentUserService.getCurrentUserId();
        PendingAdditionDTO pendingAddition = validationService.requestValidation(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Demande de validation créée avec succès", pendingAddition));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<PendingAdditionDTO>>> getPendingValidations() {
        UUID userId = currentUserService.getCurrentUserId();
        List<PendingAdditionDTO> pendingAdditions = validationService.getPendingValidations(userId);
        return ResponseEntity.ok(ApiResponse.success(pendingAdditions));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PendingAdditionDTO>> getPendingAddition(@PathVariable UUID id) {
        UUID userId = currentUserService.getCurrentUserId();
        PendingAdditionDTO pendingAddition = validationService.getPendingAdditionById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(pendingAddition));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<PendingAdditionDTO>> approveAddition(@PathVariable UUID id) {
        UUID adminId = currentUserService.getCurrentUserId();
        PendingAdditionDTO pendingAddition = validationService.approveAddition(id, adminId);
        return ResponseEntity.ok(ApiResponse.success("Ajout approuvé avec succès", pendingAddition));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<PendingAdditionDTO>> rejectAddition(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        UUID adminId = currentUserService.getCurrentUserId();
        PendingAdditionDTO pendingAddition = validationService.rejectAddition(id, adminId, reason);
        return ResponseEntity.ok(ApiResponse.success("Ajout rejeté", pendingAddition));
    }
}
