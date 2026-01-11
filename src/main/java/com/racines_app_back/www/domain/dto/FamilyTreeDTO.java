package com.racines_app_back.www.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyTreeDTO {
    private PersonDTO person;
    private List<PersonDTO> parents;
    private List<PersonDTO> grandparents;
    private List<PersonDTO> siblings;
    private List<RelationshipDTO> relationships;
}
