package com.racines_app_back.www.domain.dto;

import com.racines_app_back.www.domain.enums.RelationshipType;
import com.racines_app_back.www.domain.enums.Side;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipDTO {
    private UUID id;
    private UUID person1Id;
    private UUID person2Id;
    private RelationshipType relationshipType;
    private Side side;
    private UUID createdBy;
}
