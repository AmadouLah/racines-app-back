package com.racines_app_back.www.service;

import com.racines_app_back.www.domain.entity.User;
import com.racines_app_back.www.domain.repository.UserRepository;
import com.racines_app_back.www.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UserNotFoundException("Utilisateur non authentifié");
        }

        // Si l'authentification est OAuth2
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            if (email != null) {
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé avec l'email: " + email));
                return user.getId();
            }
        }

        // Pour l'instant, on retourne un UUID par défaut
        // Dans une implémentation complète, on devrait gérer différents types d'authentification
        throw new UserNotFoundException("Impossible de déterminer l'utilisateur actuel");
    }

    public User getCurrentUser() {
        UUID userId = getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));
    }
}
