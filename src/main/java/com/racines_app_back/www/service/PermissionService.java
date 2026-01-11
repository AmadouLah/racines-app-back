package com.racines_app_back.www.service;

import com.racines_app_back.www.domain.entity.Person;
import com.racines_app_back.www.domain.entity.User;
import com.racines_app_back.www.domain.enums.Role;
import com.racines_app_back.www.domain.repository.FamilyRelationshipRepository;
import com.racines_app_back.www.domain.repository.PersonRepository;
import com.racines_app_back.www.domain.repository.UserRepository;
import com.racines_app_back.www.exception.PermissionDeniedException;
import com.racines_app_back.www.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionService {

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final FamilyRelationshipRepository relationshipRepository;

    public boolean hasSuperAdminRole(UUID userId) {
        User user = getUserById(userId);
        return user.getRole() == Role.SUPER_ADMIN;
    }

    public boolean canViewPerson(UUID userId, UUID personId) {
        User user = getUserById(userId);
        Person person = getPersonById(personId);
        
        if (isSuperAdmin(user)) {
            return true;
        }
        
        // Personnes publiques sont visibles par tous
        if (person.getIsPublic()) {
            return true;
        }
        
        // L'utilisateur peut voir les personnes qu'il a créées
        if (person.getCreatedBy() != null && person.getCreatedBy().equals(userId)) {
            return true;
        }
        
        // L'utilisateur peut voir sa propre personne liée
        if (user.getPersonId() != null && user.getPersonId().equals(personId)) {
            return true;
        }
        
        // L'utilisateur peut voir les personnes liées à sa personne (famille proche)
        if (user.getPersonId() != null) {
            return relationshipRepository.findAllRelationshipsByPersonId(user.getPersonId())
                    .stream()
                    .anyMatch(rel -> rel.getPerson1Id().equals(personId) || rel.getPerson2Id().equals(personId));
        }
        
        return false;
    }

    public boolean canEditPerson(UUID userId, UUID personId) {
        User user = getUserById(userId);
        
        if (isSuperAdmin(user)) {
            return true;
        }
        
        getPersonById(personId);
        
        // L'utilisateur peut modifier sa propre personne liée
        return user.getPersonId() != null && user.getPersonId().equals(personId);
    }

    public boolean canAddPerson(UUID userId) {
        User user = getUserById(userId);
        return user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.VALIDATED_USER;
    }

    public void validateCanViewPerson(UUID userId, UUID personId) {
        if (!canViewPerson(userId, personId)) {
            throw new PermissionDeniedException("Vous n'avez pas la permission de visualiser cette personne");
        }
    }

    public void validateCanEditPerson(UUID userId, UUID personId) {
        if (!canEditPerson(userId, personId)) {
            throw new PermissionDeniedException("Vous n'avez pas la permission de modifier cette personne");
        }
    }

    public void validateCanAddPerson(UUID userId) {
        if (!canAddPerson(userId)) {
            throw new PermissionDeniedException("Vous n'avez pas la permission d'ajouter une personne");
        }
    }

    public void validateSuperAdmin(UUID userId) {
        if (!hasSuperAdminRole(userId)) {
            throw new PermissionDeniedException("Accès réservé aux administrateurs");
        }
    }

    private User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé: " + userId));
    }

    private Person getPersonById(UUID personId) {
        return personRepository.findById(personId)
                .orElseThrow(() -> new PermissionDeniedException("Personne non trouvée"));
    }

    private boolean isSuperAdmin(User user) {
        return user.getRole() == Role.SUPER_ADMIN;
    }
}
