package com.racines_app_back.www.service.mapper;

import com.racines_app_back.www.domain.dto.PersonDTO;
import com.racines_app_back.www.domain.dto.RelationshipDTO;
import com.racines_app_back.www.domain.entity.FamilyRelationship;
import com.racines_app_back.www.domain.entity.Person;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PersonMapper {

    public PersonDTO toDTO(Person person) {
        if (person == null) {
            return null;
        }
        return PersonDTO.builder()
                .id(person.getId())
                .nom(person.getNom())
                .prenom(person.getPrenom())
                .dateNaissance(person.getDateNaissance())
                .lieuNaissance(person.getLieuNaissance())
                .isPublic(person.getIsPublic())
                .createdBy(person.getCreatedBy())
                .validatedBy(person.getValidatedBy())
                .metadata(person.getMetadata())
                .build();
    }

    public List<PersonDTO> toDTOList(List<Person> persons) {
        return persons.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public RelationshipDTO toRelationshipDTO(FamilyRelationship relationship) {
        if (relationship == null) {
            return null;
        }
        return RelationshipDTO.builder()
                .id(relationship.getId())
                .person1Id(relationship.getPerson1Id())
                .person2Id(relationship.getPerson2Id())
                .relationshipType(relationship.getRelationshipType())
                .side(relationship.getSide())
                .createdBy(relationship.getCreatedBy())
                .build();
    }

    public List<RelationshipDTO> toRelationshipDTOList(List<FamilyRelationship> relationships) {
        return relationships.stream()
                .map(this::toRelationshipDTO)
                .collect(Collectors.toList());
    }
}
