package com.racines_app_back.www.service;

import com.racines_app_back.www.domain.dto.FamilyTreeDTO;
import com.racines_app_back.www.domain.dto.PersonCreateDTO;
import com.racines_app_back.www.domain.dto.PersonDTO;
import com.racines_app_back.www.domain.dto.PersonUpdateDTO;
import com.racines_app_back.www.domain.dto.RelationshipDTO;
import com.racines_app_back.www.domain.entity.FamilyRelationship;
import com.racines_app_back.www.domain.entity.Person;
import com.racines_app_back.www.domain.enums.RelationshipType;
import com.racines_app_back.www.domain.repository.FamilyRelationshipRepository;
import com.racines_app_back.www.domain.repository.PersonRepository;
import com.racines_app_back.www.exception.InvalidRelationshipException;
import com.racines_app_back.www.exception.PersonNotFoundException;
import com.racines_app_back.www.service.mapper.PersonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PersonService {

    private final PersonRepository personRepository;
    private final FamilyRelationshipRepository relationshipRepository;
    private final PermissionService permissionService;
    private final PersonMapper personMapper;

    public PersonDTO createPerson(PersonCreateDTO dto, UUID userId) {
        permissionService.validateCanAddPerson(userId);
        
        Person person = Person.builder()
                .nom(dto.getNom())
                .prenom(dto.getPrenom())
                .dateNaissance(dto.getDateNaissance())
                .lieuNaissance(dto.getLieuNaissance())
                .isPublic(dto.getIsPublic() != null ? dto.getIsPublic() : false)
                .createdBy(userId != null ? userId : dto.getCreatedBy())
                .metadata(dto.getMetadata())
                .build();

        Person saved = personRepository.save(person);
        return personMapper.toDTO(saved);
    }

    public PersonDTO getPersonById(UUID personId, UUID userId) {
        permissionService.validateCanViewPerson(userId, personId);
        
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new PersonNotFoundException("Personne non trouvée: " + personId));
        
        return personMapper.toDTO(person);
    }

    public FamilyTreeDTO getFamilyTree(UUID personId, UUID userId) {
        permissionService.validateCanViewPerson(userId, personId);
        
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new PersonNotFoundException("Personne non trouvée: " + personId));

        List<FamilyRelationship> allRelationships = relationshipRepository.findAllRelationshipsByPersonId(personId);
        
        Set<UUID> relatedPersonIds = new HashSet<>();
        relatedPersonIds.add(personId);
        
        for (FamilyRelationship rel : allRelationships) {
            relatedPersonIds.add(rel.getPerson1Id());
            relatedPersonIds.add(rel.getPerson2Id());
        }

        List<Person> relatedPersons = personRepository.findAllById(relatedPersonIds);
        Map<UUID, Person> personMap = relatedPersons.stream()
                .collect(Collectors.toMap(Person::getId, p -> p));

        List<PersonDTO> parents = new ArrayList<>();
        List<PersonDTO> grandparents = new ArrayList<>();
        List<PersonDTO> siblings = new ArrayList<>();

        for (FamilyRelationship rel : allRelationships) {
            UUID otherPersonId = rel.getPerson1Id().equals(personId) ? rel.getPerson2Id() : rel.getPerson1Id();
            Person otherPerson = personMap.get(otherPersonId);
            
            if (otherPerson != null) {
                PersonDTO otherDTO = personMapper.toDTO(otherPerson);
                
                if (rel.getRelationshipType() == RelationshipType.PARENT) {
                    parents.add(otherDTO);
                } else if (rel.getRelationshipType() == RelationshipType.SIBLING) {
                    siblings.add(otherDTO);
                }
            }
        }

        // Récupérer les grands-parents via les parents
        for (PersonDTO parent : parents) {
            List<FamilyRelationship> parentRelationships = relationshipRepository
                    .findAllRelationshipsByPersonId(parent.getId());
            for (FamilyRelationship rel : parentRelationships) {
                if (rel.getRelationshipType() == RelationshipType.PARENT) {
                    UUID grandparentId = rel.getPerson1Id().equals(parent.getId()) 
                            ? rel.getPerson2Id() 
                            : rel.getPerson1Id();
                    Person grandparent = personMap.get(grandparentId);
                    if (grandparent != null) {
                        PersonDTO grandparentDTO = personMapper.toDTO(grandparent);
                        if (!grandparents.contains(grandparentDTO)) {
                            grandparents.add(grandparentDTO);
                        }
                    }
                }
            }
        }

        List<RelationshipDTO> relationships = personMapper.toRelationshipDTOList(allRelationships);

        return FamilyTreeDTO.builder()
                .person(personMapper.toDTO(person))
                .parents(parents)
                .grandparents(grandparents)
                .siblings(siblings)
                .relationships(relationships)
                .build();
    }

    public PersonDTO updatePerson(UUID personId, PersonUpdateDTO dto, UUID userId) {
        permissionService.validateCanEditPerson(userId, personId);
        
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new PersonNotFoundException("Personne non trouvée: " + personId));

        if (dto.getNom() != null) person.setNom(dto.getNom());
        if (dto.getPrenom() != null) person.setPrenom(dto.getPrenom());
        if (dto.getDateNaissance() != null) person.setDateNaissance(dto.getDateNaissance());
        if (dto.getLieuNaissance() != null) person.setLieuNaissance(dto.getLieuNaissance());
        if (dto.getMetadata() != null) person.setMetadata(dto.getMetadata());

        Person updated = personRepository.save(person);
        return personMapper.toDTO(updated);
    }

    public RelationshipDTO addRelationship(UUID person1Id, UUID person2Id, RelationshipType type, UUID userId) {
        if (person1Id.equals(person2Id)) {
            throw new InvalidRelationshipException("Une personne ne peut pas être en relation avec elle-même");
        }

        Person person1 = personRepository.findById(person1Id)
                .orElseThrow(() -> new PersonNotFoundException("Personne 1 non trouvée: " + person1Id));
        Person person2 = personRepository.findById(person2Id)
                .orElseThrow(() -> new PersonNotFoundException("Personne 2 non trouvée: " + person2Id));

        permissionService.validateCanEditPerson(userId, person1Id);

        // Vérifier si la relation existe déjà
        Optional<FamilyRelationship> existing = relationshipRepository
                .findByPerson1IdAndPerson2IdAndRelationshipType(person1Id, person2Id, type);
        
        if (existing.isPresent()) {
            throw new InvalidRelationshipException("Cette relation existe déjà");
        }

        FamilyRelationship relationship = FamilyRelationship.builder()
                .person1Id(person1Id)
                .person2Id(person2Id)
                .relationshipType(type)
                .createdBy(userId)
                .build();

        FamilyRelationship saved = relationshipRepository.save(relationship);
        return personMapper.toRelationshipDTO(saved);
    }

    public List<PersonDTO> getAncestors(UUID personId, UUID userId, int maxDepth) {
        permissionService.validateCanViewPerson(userId, personId);
        return getAncestorsRecursive(personId, userId, maxDepth, new HashSet<>());
    }

    private List<PersonDTO> getAncestorsRecursive(UUID personId, UUID userId, int depth, Set<UUID> visited) {
        if (depth <= 0 || visited.contains(personId)) {
            return new ArrayList<>();
        }
        visited.add(personId);

        List<FamilyRelationship> parentRelations = relationshipRepository
                .findByPerson1IdAndRelationshipType(personId, RelationshipType.PARENT);
        
        List<PersonDTO> ancestors = new ArrayList<>();
        for (FamilyRelationship rel : parentRelations) {
            UUID parentId = rel.getPerson2Id();
            Person parent = personRepository.findById(parentId).orElse(null);
            if (parent != null && permissionService.canViewPerson(userId, parentId)) {
                ancestors.add(personMapper.toDTO(parent));
                ancestors.addAll(getAncestorsRecursive(parentId, userId, depth - 1, visited));
            }
        }
        return ancestors;
    }

    public List<PersonDTO> getDescendants(UUID personId, UUID userId, int maxDepth) {
        permissionService.validateCanViewPerson(userId, personId);
        return getDescendantsRecursive(personId, userId, maxDepth, new HashSet<>());
    }

    private List<PersonDTO> getDescendantsRecursive(UUID personId, UUID userId, int depth, Set<UUID> visited) {
        if (depth <= 0 || visited.contains(personId)) {
            return new ArrayList<>();
        }
        visited.add(personId);

        List<FamilyRelationship> childRelations = relationshipRepository
                .findByPerson1IdAndRelationshipType(personId, RelationshipType.CHILD);
        
        List<PersonDTO> descendants = new ArrayList<>();
        for (FamilyRelationship rel : childRelations) {
            UUID childId = rel.getPerson2Id();
            Person child = personRepository.findById(childId).orElse(null);
            if (child != null && permissionService.canViewPerson(userId, childId)) {
                descendants.add(personMapper.toDTO(child));
                descendants.addAll(getDescendantsRecursive(childId, userId, depth - 1, visited));
            }
        }
        return descendants;
    }
}
