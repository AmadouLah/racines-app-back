package com.racines_app_back.www.service;

import com.racines_app_back.www.domain.entity.User;
import com.racines_app_back.www.domain.enums.Role;
import com.racines_app_back.www.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataInitializationService implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "amadoulandoure004@gmail.com";
    private static final String ADMIN_NOM = "Landouré";
    private static final String ADMIN_PRENOM = "Amadou";
    private static final String ADMIN_PASSWORD = "F4@655#&3%@&27^!3*3o";
    private static final LocalDate ADMIN_DATE_NAISSANCE = LocalDate.of(2000, 1, 1);

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    @Override
    public void run(String... args) {
        initializeAdminUser();
    }

    private void initializeAdminUser() {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            log.debug("L'administrateur avec l'email {} existe déjà. Aucune initialisation nécessaire.", ADMIN_EMAIL);
            return;
        }

        String passwordHash = passwordService.encodePassword(ADMIN_PASSWORD);

        User adminUser = User.builder()
                .email(ADMIN_EMAIL)
                .nom(ADMIN_NOM)
                .prenom(ADMIN_PRENOM)
                .dateNaissance(ADMIN_DATE_NAISSANCE)
                .role(Role.SUPER_ADMIN)
                .passwordHash(passwordHash)
                .build();

        userRepository.save(adminUser);
        log.info("Administrateur initialisé avec succès: {} {}", ADMIN_PRENOM, ADMIN_NOM);
    }
}
