package com.racines_app_back.www.service;

import com.racines_app_back.www.domain.dto.ProfileClaimCreateDTO;
import com.racines_app_back.www.domain.dto.ProfileClaimDTO;
import com.racines_app_back.www.domain.entity.ProfileClaim;
import com.racines_app_back.www.domain.entity.User;
import com.racines_app_back.www.domain.enums.ClaimStatus;
import com.racines_app_back.www.domain.enums.Role;
import com.racines_app_back.www.domain.repository.ProfileClaimRepository;
import com.racines_app_back.www.domain.repository.UserRepository;
import com.racines_app_back.www.exception.UserNotFoundException;
import com.racines_app_back.www.exception.ValidationException;
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
public class ProfileClaimService {

    private final ProfileClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final EmailService emailService;

    public ProfileClaimDTO createClaim(ProfileClaimCreateDTO dto, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé: " + userId));

        // Vérifier si l'utilisateur a déjà une personne liée
        if (user.getPersonId() != null) {
            throw new ValidationException("Vous avez déjà un profil lié");
        }

        // Vérifier s'il existe déjà une revendication en attente
        claimRepository.findByPersonIdAndUserId(dto.getPersonId(), userId)
                .ifPresent(claim -> {
                    if (claim.getStatus() == ClaimStatus.PENDING) {
                        throw new ValidationException("Une revendication est déjà en attente pour ce profil");
                    }
                });

        ProfileClaim claim = ProfileClaim.builder()
                .personId(dto.getPersonId())
                .userId(userId)
                .email(dto.getEmail())
                .nom(dto.getNom())
                .prenom(dto.getPrenom())
                .dateNaissance(dto.getDateNaissance())
                .status(ClaimStatus.PENDING)
                .build();

        ProfileClaim saved = claimRepository.save(claim);

        // Envoyer notification aux admins
        String claimantName = buildFullName(dto.getPrenom(), dto.getNom());
        List<User> admins = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.SUPER_ADMIN)
                .collect(Collectors.toList());
        
        for (User admin : admins) {
            emailService.sendClaimNotification(admin.getEmail(), claimantName, claimantName);
        }

        return toDTO(saved);
    }

    public ProfileClaimDTO approveClaim(UUID claimId, UUID adminId) {
        ProfileClaim claim = validateAndGetPendingClaim(claimId, adminId);
        User user = getUserById(claim.getUserId());

        user.setPersonId(claim.getPersonId());
        user.setRole(Role.VALIDATED_USER);
        userRepository.save(user);

        claim.setStatus(ClaimStatus.APPROVED);
        ProfileClaim saved = updateClaimProcessing(claim, adminId);

        emailService.sendApprovalNotification(user.getEmail(), buildFullName(claim.getPrenom(), claim.getNom()), true);

        return toDTO(saved);
    }

    public ProfileClaimDTO rejectClaim(UUID claimId, UUID adminId, String reason) {
        ProfileClaim claim = validateAndGetPendingClaim(claimId, adminId);
        User user = getUserById(claim.getUserId());

        claim.setStatus(ClaimStatus.REJECTED);
        claim.setRejectionReason(reason);
        ProfileClaim saved = updateClaimProcessing(claim, adminId);

        emailService.sendRejectionNotification(user.getEmail(), buildFullName(claim.getPrenom(), claim.getNom()), reason, true);

        return toDTO(saved);
    }

    public List<ProfileClaimDTO> getClaimsByStatus(ClaimStatus status, UUID userId) {
        boolean isSuperAdmin = permissionService.hasSuperAdminRole(userId);
        
        if (status == null) {
            return (isSuperAdmin 
                    ? claimRepository.findAll() 
                    : claimRepository.findByUserId(userId))
                    .stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
        }

        return (isSuperAdmin 
                ? claimRepository.findByStatus(status) 
                : claimRepository.findByUserIdAndStatus(userId, status))
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ProfileClaimDTO getClaimById(UUID claimId, UUID userId) {
        ProfileClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ValidationException("Revendication non trouvée: " + claimId));

        if (!permissionService.hasSuperAdminRole(userId) && !claim.getUserId().equals(userId)) {
            throw new ValidationException("Vous n'avez pas accès à cette revendication");
        }

        return toDTO(claim);
    }

    private ProfileClaim validateAndGetPendingClaim(UUID claimId, UUID adminId) {
        permissionService.validateSuperAdmin(adminId);
        ProfileClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ValidationException("Revendication non trouvée: " + claimId));
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new ValidationException("Cette revendication a déjà été traitée");
        }
        return claim;
    }

    private ProfileClaim updateClaimProcessing(ProfileClaim claim, UUID adminId) {
        claim.setProcessedBy(adminId);
        claim.setProcessedAt(LocalDateTime.now());
        return claimRepository.save(claim);
    }

    private User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));
    }

    private String buildFullName(String prenom, String nom) {
        return prenom + " " + nom;
    }

    private ProfileClaimDTO toDTO(ProfileClaim claim) {
        return ProfileClaimDTO.builder()
                .id(claim.getId())
                .personId(claim.getPersonId())
                .userId(claim.getUserId())
                .email(claim.getEmail())
                .nom(claim.getNom())
                .prenom(claim.getPrenom())
                .dateNaissance(claim.getDateNaissance())
                .status(claim.getStatus())
                .processedBy(claim.getProcessedBy())
                .processedAt(claim.getProcessedAt())
                .rejectionReason(claim.getRejectionReason())
                .createdAt(claim.getCreatedAt())
                .build();
    }
}
