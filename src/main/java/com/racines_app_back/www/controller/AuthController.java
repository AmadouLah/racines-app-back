package com.racines_app_back.www.controller;

import com.racines_app_back.www.domain.dto.ApiResponse;
import com.racines_app_back.www.domain.entity.User;
import com.racines_app_back.www.domain.enums.Role;
import com.racines_app_back.www.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    @GetMapping("/success")
    public ResponseEntity<ApiResponse<Map<String, Object>>> authSuccess(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            return ResponseEntity.ok(ApiResponse.error("Non authentifié"));
        }

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        
        if (email == null) {
            return ResponseEntity.ok(ApiResponse.error("Email non disponible"));
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            String[] nameParts = name != null ? name.split(" ", 2) : new String[]{"", ""};
            User newUser = User.builder()
                    .email(email)
                    .nom(nameParts.length > 1 ? nameParts[1] : "")
                    .prenom(nameParts[0])
                    .dateNaissance(LocalDate.now().minusYears(25))
                    .role(Role.PENDING_USER)
                    .oauth2ProviderId(oauth2User.getName())
                    .build();
            return userRepository.save(newUser);
        });

        Map<String, Object> userInfo = Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "nom", user.getNom(),
                "prenom", user.getPrenom(),
                "role", user.getRole().name()
        );

        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    @GetMapping("/failure")
    public ResponseEntity<ApiResponse<Object>> authFailure() {
        return ResponseEntity.ok(ApiResponse.error("Échec de l'authentification"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            return ResponseEntity.ok(ApiResponse.error("Non authentifié"));
        }

        String email = oauth2User.getAttribute("email");
        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error("Utilisateur non trouvé"));
        }

        Map<String, Object> userInfo = Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "nom", user.getNom(),
                "prenom", user.getPrenom(),
                "role", user.getRole().name()
        );

        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }
}
