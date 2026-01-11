package com.racines_app_back.www.domain.dto;

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
public class ValidationRequestDTO {
    @NotNull(message = "L'ID de la personne est obligatoire")
    private UUID personId;
}
