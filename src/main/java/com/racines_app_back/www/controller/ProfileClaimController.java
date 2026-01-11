package com.racines_app_back.www.controller;

import com.racines_app_back.www.domain.dto.ApiResponse;
import com.racines_app_back.www.domain.dto.ProfileClaimCreateDTO;
import com.racines_app_back.www.domain.dto.ProfileClaimDTO;
import com.racines_app_back.www.domain.enums.ClaimStatus;
import com.racines_app_back.www.service.CurrentUserService;
import com.racines_app_back.www.service.ProfileClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile-claims")
@RequiredArgsConstructor
public class ProfileClaimController {

    private final ProfileClaimService claimService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProfileClaimDTO>> createClaim(@Valid @RequestBody ProfileClaimCreateDTO dto) {
        UUID userId = currentUserService.getCurrentUserId();
        ProfileClaimDTO claim = claimService.createClaim(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Revendication créée avec succès", claim));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProfileClaimDTO>>> getClaims(
            @RequestParam(required = false) ClaimStatus status) {
        UUID userId = currentUserService.getCurrentUserId();
        List<ProfileClaimDTO> claims = claimService.getClaimsByStatus(status, userId);
        return ResponseEntity.ok(ApiResponse.success(claims));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProfileClaimDTO>> getClaim(@PathVariable UUID id) {
        UUID userId = currentUserService.getCurrentUserId();
        ProfileClaimDTO claim = claimService.getClaimById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(claim));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ProfileClaimDTO>> approveClaim(@PathVariable UUID id) {
        UUID adminId = currentUserService.getCurrentUserId();
        ProfileClaimDTO claim = claimService.approveClaim(id, adminId);
        return ResponseEntity.ok(ApiResponse.success("Revendication approuvée avec succès", claim));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ProfileClaimDTO>> rejectClaim(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        UUID adminId = currentUserService.getCurrentUserId();
        ProfileClaimDTO claim = claimService.rejectClaim(id, adminId, reason);
        return ResponseEntity.ok(ApiResponse.success("Revendication rejetée", claim));
    }
}
