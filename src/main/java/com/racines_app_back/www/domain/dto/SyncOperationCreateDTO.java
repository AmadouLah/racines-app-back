package com.racines_app_back.www.domain.dto;

import com.racines_app_back.www.domain.enums.EntityType;
import com.racines_app_back.www.domain.enums.OperationType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncOperationCreateDTO {
    @NotNull(message = "Le type d'opération est obligatoire")
    private OperationType operationType;
    
    @NotNull(message = "Le type d'entité est obligatoire")
    private EntityType entityType;
    
    private UUID entityId;
    private String payload;
}
