package com.racines_app_back.www.domain.dto;

import com.racines_app_back.www.domain.enums.ValidationStatus;
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
public class PendingAdditionDTO {
    private UUID id;
    private UUID personId;
    private PersonDTO person;
    private UUID requestedBy;
    private ValidationStatus status;
    private UUID processedBy;
    private LocalDateTime processedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
}
