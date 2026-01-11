package com.racines_app_back.www.service;

import com.racines_app_back.www.domain.dto.PendingAdditionDTO;
import com.racines_app_back.www.domain.dto.ValidationRequestDTO;
import com.racines_app_back.www.domain.entity.PendingAddition;
import com.racines_app_back.www.domain.entity.Person;
import com.racines_app_back.www.domain.entity.User;
import com.racines_app_back.www.domain.enums.Role;
import com.racines_app_back.www.domain.enums.ValidationStatus;
import com.racines_app_back.www.domain.repository.PendingAdditionRepository;
import com.racines_app_back.www.domain.repository.PersonRepository;
import com.racines_app_back.www.domain.repository.UserRepository;
import com.racines_app_back.www.exception.PersonNotFoundException;
import com.racines_app_back.www.exception.UserNotFoundException;
import com.racines_app_back.www.exception.ValidationException;
import com.racines_app_back.www.service.mapper.PersonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ValidationService {

    private final PendingAdditionRepository pendingAdditionRepository;
    private final PersonRepository personRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final EmailService emailService;
    private final PersonMapper personMapper;

    public PendingAdditionDTO requestValidation(ValidationRequestDTO dto, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé: " + userId));

        if (user.getRole() != Role.VALIDATED_USER) {
            throw new ValidationException("Seuls les utilisateurs validés peuvent demander une validation");
        }

        Person person = personRepository.findById(dto.getPersonId())
                .orElseThrow(() -> new PersonNotFoundException("Personne non trouvée: " + dto.getPersonId()));

        if (person.getIsPublic()) {
            throw new ValidationException("Cette personne est déjà publique");
        }

        if (!person.getCreatedBy().equals(userId)) {
            throw new ValidationException("Vous ne pouvez demander la validation que pour les personnes que vous avez créées");
        }

        pendingAdditionRepository.findByPersonIdAndRequestedBy(dto.getPersonId(), userId)
                .ifPresent(existing -> {
                    if (existing.getStatus() == ValidationStatus.PENDING) {
                        throw new ValidationException("Une demande de validation est déjà en attente pour cette personne");
                    }
                });

        PendingAddition pendingAddition = PendingAddition.builder()
                .personId(dto.getPersonId())
                .requestedBy(userId)
                .status(ValidationStatus.PENDING)
                .build();

        PendingAddition saved = pendingAdditionRepository.save(pendingAddition);

        // Envoyer notification aux admins
        List<User> admins = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.SUPER_ADMIN)
                .collect(Collectors.toList());

        String requesterName = user.getPrenom() + " " + user.getNom();
        String personName = person.getPrenom() + " " + person.getNom();
        
        for (User admin : admins) {
            emailService.sendValidationRequest(admin.getEmail(), requesterName, personName);
        }

        return toDTO(saved, person);
    }

    public PendingAdditionDTO approveAddition(UUID pendingAdditionId, UUID adminId) {
        permissionService.validateSuperAdmin(adminId);

        PendingAddition pendingAddition = pendingAdditionRepository.findById(pendingAdditionId)
                .orElseThrow(() -> new ValidationException("Demande de validation non trouvée: " + pendingAdditionId));

        if (pendingAddition.getStatus() != ValidationStatus.PENDING) {
            throw new ValidationException("Cette demande a déjà été traitée");
        }

        Person person = personRepository.findById(pendingAddition.getPersonId())
                .orElseThrow(() -> new PersonNotFoundException("Personne non trouvée"));

        person.setIsPublic(true);
        person.setValidatedBy(adminId);
        personRepository.save(person);

        pendingAddition.setStatus(ValidationStatus.APPROVED);
        pendingAddition.setProcessedBy(adminId);
        pendingAddition.setProcessedAt(LocalDateTime.now());
        PendingAddition saved = pendingAdditionRepository.save(pendingAddition);

        User requester = userRepository.findById(pendingAddition.getRequestedBy())
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));
        
        emailService.sendApprovalNotification(
                requester.getEmail(),
                person.getPrenom() + " " + person.getNom(),
                false
        );

        return toDTO(saved, person);
    }

    public PendingAdditionDTO rejectAddition(UUID pendingAdditionId, UUID adminId, String reason) {
        permissionService.validateSuperAdmin(adminId);

        PendingAddition pendingAddition = pendingAdditionRepository.findById(pendingAdditionId)
                .orElseThrow(() -> new ValidationException("Demande de validation non trouvée: " + pendingAdditionId));

        if (pendingAddition.getStatus() != ValidationStatus.PENDING) {
            throw new ValidationException("Cette demande a déjà été traitée");
        }

        pendingAddition.setStatus(ValidationStatus.REJECTED);
        pendingAddition.setProcessedBy(adminId);
        pendingAddition.setProcessedAt(LocalDateTime.now());
        pendingAddition.setRejectionReason(reason);
        PendingAddition saved = pendingAdditionRepository.save(pendingAddition);

        Person person = personRepository.findById(pendingAddition.getPersonId())
                .orElseThrow(() -> new PersonNotFoundException("Personne non trouvée"));
        
        User requester = userRepository.findById(pendingAddition.getRequestedBy())
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));
        
        emailService.sendRejectionNotification(
                requester.getEmail(),
                person.getPrenom() + " " + person.getNom(),
                reason,
                false
        );

        return toDTO(saved, person);
    }

    public List<PendingAdditionDTO> getPendingValidations(UUID userId) {
        if (permissionService.hasSuperAdminRole(userId)) {
            return pendingAdditionRepository.findByStatus(ValidationStatus.PENDING).stream()
                    .map(pa -> {
                        Person person = personRepository.findById(pa.getPersonId())
                                .orElseThrow(() -> new PersonNotFoundException("Personne non trouvée"));
                        return toDTO(pa, person);
                    })
                    .collect(Collectors.toList());
        }

        return pendingAdditionRepository.findByRequestedByAndStatus(userId, ValidationStatus.PENDING).stream()
                .map(pa -> {
                    Person person = personRepository.findById(pa.getPersonId())
                            .orElseThrow(() -> new PersonNotFoundException("Personne non trouvée"));
                    return toDTO(pa, person);
                })
                .collect(Collectors.toList());
    }

    public PendingAdditionDTO getPendingAdditionById(UUID pendingAdditionId, UUID userId) {
        PendingAddition pendingAddition = pendingAdditionRepository.findById(pendingAdditionId)
                .orElseThrow(() -> new ValidationException("Demande de validation non trouvée: " + pendingAdditionId));

        if (!permissionService.hasSuperAdminRole(userId) && !pendingAddition.getRequestedBy().equals(userId)) {
            throw new ValidationException("Vous n'avez pas accès à cette demande");
        }

        Person person = personRepository.findById(pendingAddition.getPersonId())
                .orElseThrow(() -> new PersonNotFoundException("Personne non trouvée"));

        return toDTO(pendingAddition, person);
    }

    private PendingAdditionDTO toDTO(PendingAddition pendingAddition, Person person) {
        return PendingAdditionDTO.builder()
                .id(pendingAddition.getId())
                .personId(pendingAddition.getPersonId())
                .person(personMapper.toDTO(person))
                .requestedBy(pendingAddition.getRequestedBy())
                .status(pendingAddition.getStatus())
                .processedBy(pendingAddition.getProcessedBy())
                .processedAt(pendingAddition.getProcessedAt())
                .rejectionReason(pendingAddition.getRejectionReason())
                .createdAt(pendingAddition.getCreatedAt())
                .build();
    }
}
