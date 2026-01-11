package com.racines_app_back.www.domain.dto;

import com.racines_app_back.www.domain.enums.ClaimStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileClaimDTO {
    private UUID id;
    private UUID personId;
    private UUID userId;
    private String email;
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private ClaimStatus status;
    private UUID processedBy;
    private LocalDateTime processedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
}
