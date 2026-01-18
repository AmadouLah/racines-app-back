package com.racines_app_back.www.controller;

import com.racines_app_back.www.domain.dto.*;
import com.racines_app_back.www.domain.entity.User;
import com.racines_app_back.www.domain.enums.LoginType;
import com.racines_app_back.www.domain.enums.Role;
import com.racines_app_back.www.domain.repository.UserRepository;
import com.racines_app_back.www.exception.UserNotFoundException;
import com.racines_app_back.www.service.OTPService;
import com.racines_app_back.www.service.PasswordService;
import com.racines_app_back.www.util.BrevoEmailService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final OTPService otpService;
    private final PasswordService passwordService;
    private final BrevoEmailService emailService;

    @GetMapping("/success")
    public ResponseEntity<ApiResponse<Map<String, Object>>> authSuccess(
            @AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            return ResponseEntity.ok(ApiResponse.error("Non authentifié"));
        }

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        if (email == null) {
            return ResponseEntity.ok(ApiResponse.error("Email non disponible"));
        }

        User existingUser = userRepository.findByEmail(email).orElse(null);

        if (existingUser != null && existingUser.getRole() == Role.SUPER_ADMIN) {
            log.warn("Tentative de connexion Google OAuth bloquée pour l'administrateur: {}", email);
            return ResponseEntity.ok(ApiResponse.error(
                    "Les administrateurs ne peuvent pas se connecter via Google. Utilisez la connexion manuelle avec votre mot de passe."));
        }

        User user = existingUser;
        if (user == null) {
            String[] nameParts = name != null ? name.split(" ", 2) : new String[] { "", "" };
            user = User.builder()
                    .email(email)
                    .nom(nameParts.length > 1 ? nameParts[1] : "")
                    .prenom(nameParts[0])
                    .dateNaissance(LocalDate.now().minusYears(25))
                    .role(Role.PENDING_USER)
                    .oauth2ProviderId(oauth2User.getName())
                    .build();
            user = userRepository.save(user);
        }

        Map<String, Object> userInfo = Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "nom", user.getNom(),
                "prenom", user.getPrenom(),
                "role", user.getRole().name());

        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    @GetMapping("/failure")
    public ResponseEntity<ApiResponse<Object>> authFailure() {
        return ResponseEntity.ok(ApiResponse.error("Échec de l'authentification"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpSession session) {

        User user = null;

        if (oauth2User != null) {
            String email = oauth2User.getAttribute("email");
            user = userRepository.findByEmail(email).orElse(null);
        } else if (session.getAttribute("userId") != null) {
            user = userRepository.findById((java.util.UUID) session.getAttribute("userId"))
                    .orElse(null);
        }

        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error("Non authentifié"));
        }

        Map<String, Object> userInfo = Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "nom", user.getNom(),
                "prenom", user.getPrenom(),
                "role", user.getRole().name());

        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    @PostMapping("/login/initiate")
    public ResponseEntity<ApiResponse<Map<String, String>>> initiateLogin(
            @Valid @RequestBody LoginInitiateDTO request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            // Inscription : l'utilisateur n'existe pas, on crée un nouveau compte
            String otpCode = otpService.generateOTP();

            user = User.builder()
                    .email(request.getEmail())
                    .nom("")
                    .prenom("")
                    .dateNaissance(LocalDate.now().minusYears(25))
                    .role(Role.PENDING_USER)
                    .otpCode(otpCode)
                    .otpExpiresAt(otpService.getExpiryTime())
                    .build();
            user = userRepository.save(user);

            log.info("Nouveau compte créé pour l'inscription: email={}", user.getEmail());

            try {
                emailService.sendOTP(user.getEmail(), otpCode);
                return ResponseEntity.ok(ApiResponse.success(Map.of(
                        "type", LoginType.OTP.name(),
                        "message", "Code OTP envoyé à votre adresse email. Veuillez compléter votre inscription.")));
            } catch (RuntimeException e) {
                log.error("Erreur lors de l'envoi de l'OTP à {}: {}", user.getEmail(), e.getMessage());
                return ResponseEntity.ok(ApiResponse.error(
                        "Erreur lors de l'envoi de l'email. Veuillez vérifier la configuration du service d'email."));
            }
        }

        // Connexion : l'utilisateur existe déjà
        log.debug("Tentative de connexion pour l'utilisateur: email={}, role={}", user.getEmail(), user.getRole());

        if (user.getRole() == Role.SUPER_ADMIN) {
            if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
                log.error("L'administrateur {} n'a pas de mot de passe configuré", user.getEmail());
                return ResponseEntity.ok(ApiResponse
                        .error("L'administrateur n'a pas de mot de passe configuré. Veuillez contacter le support."));
            }
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "type", LoginType.PASSWORD.name(),
                    "message", "Administrateur détecté. Veuillez entrer votre mot de passe.")));
        } else {
            // Connexion avec OTP pour les utilisateurs normaux
            String otpCode = otpService.generateOTP();
            user.setOtpCode(otpCode);
            user.setOtpExpiresAt(otpService.getExpiryTime());
            userRepository.save(user);

            try {
                emailService.sendOTP(user.getEmail(), otpCode);
                return ResponseEntity.ok(ApiResponse.success(Map.of(
                        "type", LoginType.OTP.name(),
                        "message", "Code OTP envoyé à votre adresse email.")));
            } catch (RuntimeException e) {
                log.error("Erreur lors de l'envoi de l'OTP à {}: {}", user.getEmail(), e.getMessage());
                return ResponseEntity.ok(ApiResponse.error(
                        "Erreur lors de l'envoi de l'email. Veuillez vérifier la configuration du service d'email."));
            }
        }
    }

    @PostMapping("/login/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOTP(
            @Valid @RequestBody LoginVerifyOTPDTO request,
            HttpSession session) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));

        if (!otpService.verifyOTP(request.getOtpCode(), user.getOtpCode(), user.getOtpExpiresAt())) {
            return ResponseEntity.ok(ApiResponse.error("Code OTP invalide ou expiré"));
        }

        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        userRepository.save(user);

        session.setAttribute("userId", user.getId());
        session.setAttribute("userEmail", user.getEmail());

        Map<String, Object> userInfo = Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "nom", user.getNom(),
                "prenom", user.getPrenom(),
                "role", user.getRole().name());

        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    @PostMapping("/login/verify-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyPassword(
            @Valid @RequestBody LoginVerifyPasswordDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));

        if (user.getRole() != Role.SUPER_ADMIN) {
            return ResponseEntity.ok(ApiResponse.error("Cette méthode est réservée aux administrateurs"));
        }

        if (!passwordService.matches(request.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.ok(ApiResponse.error("Mot de passe incorrect"));
        }

        // Mot de passe correct : générer et envoyer l'OTP
        String otpCode = otpService.generateOTP();
        user.setOtpCode(otpCode);
        user.setOtpExpiresAt(otpService.getExpiryTime());
        userRepository.save(user);

        try {
            emailService.sendOTP(user.getEmail(), otpCode);
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "type", LoginType.OTP.name(),
                    "message", "Mot de passe correct. Code OTP envoyé à votre adresse email.")));
        } catch (RuntimeException e) {
            log.error("Erreur lors de l'envoi de l'OTP à {}: {}", user.getEmail(), e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(
                    "Erreur lors de l'envoi de l'email. Veuillez vérifier la configuration du service d'email."));
        }
    }

    @PostMapping("/admin/request-otp")
    public ResponseEntity<ApiResponse<Map<String, String>>> requestOTPForAdmin(
            @Valid @RequestBody LoginInitiateDTO request,
            HttpSession session) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));

        if (user.getRole() != Role.SUPER_ADMIN) {
            return ResponseEntity.ok(ApiResponse.error("Cette fonctionnalité est réservée aux administrateurs"));
        }

        String otpCode = otpService.generateOTP();
        user.setOtpCode(otpCode);
        user.setOtpExpiresAt(otpService.getExpiryTime());
        userRepository.save(user);

        emailService.sendOTP(user.getEmail(), otpCode);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "message", "Code OTP envoyé à votre adresse email.")));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(ApiResponse.success("Déconnexion réussie"));
    }
}
