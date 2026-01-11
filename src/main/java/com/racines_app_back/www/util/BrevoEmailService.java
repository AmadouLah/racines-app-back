package com.racines_app_back.www.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class BrevoEmailService {

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${brevo.api.url}")
    private String apiUrl;

    @Value("${app.mail.from}")
    private String fromEmail;

    private final WebClient webClient;

    public void sendEmail(String to, String subject, String htmlContent) {
        try {
            Map<String, Object> emailData = new HashMap<>();
            emailData.put("sender", Map.of("email", fromEmail));
            emailData.put("to", List.of(Map.of("email", to)));
            emailData.put("subject", subject);
            emailData.put("htmlContent", htmlContent);

            webClient.post()
                    .uri(apiUrl + "/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(emailData)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Email envoyé avec succès à: {}", to);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email à {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }

    public void sendClaimNotification(String adminEmail, String claimantName, String personName) {
        String subject = "Nouvelle revendication de profil - " + personName;
        String htmlContent = String.format(
            "<html><body>" +
            "<h2>Nouvelle revendication de profil</h2>" +
            "<p>Une nouvelle revendication de profil a été soumise par <strong>%s</strong> pour le profil de <strong>%s</strong>.</p>" +
            "<p>Veuillez vous connecter à l'application pour valider ou rejeter cette demande.</p>" +
            "</body></html>",
            claimantName, personName
        );
        sendEmail(adminEmail, subject, htmlContent);
    }

    public void sendValidationRequest(String adminEmail, String requesterName, String personName) {
        String subject = "Demande de validation - " + personName;
        String htmlContent = String.format(
            "<html><body>" +
            "<h2>Demande de validation</h2>" +
            "<p><strong>%s</strong> a demandé la validation pour ajouter <strong>%s</strong> à l'arbre généalogique public.</p>" +
            "<p>Veuillez vous connecter à l'application pour valider ou rejeter cette demande.</p>" +
            "</body></html>",
            requesterName, personName
        );
        sendEmail(adminEmail, subject, htmlContent);
    }

    public void sendApprovalNotification(String userEmail, String personName, boolean isClaim) {
        String subject = isClaim ? "Revendication approuvée - " + personName : "Validation approuvée - " + personName;
        String htmlContent = String.format(
            "<html><body>" +
            "<h2>Demande approuvée</h2>" +
            "<p>Votre demande concernant <strong>%s</strong> a été approuvée.</p>" +
            "<p>Vous pouvez maintenant accéder à votre profil dans l'application.</p>" +
            "</body></html>",
            personName
        );
        sendEmail(userEmail, subject, htmlContent);
    }

    public void sendRejectionNotification(String userEmail, String personName, String reason, boolean isClaim) {
        String subject = isClaim ? "Revendication rejetée - " + personName : "Validation rejetée - " + personName;
        String htmlContent = String.format(
            "<html><body>" +
            "<h2>Demande rejetée</h2>" +
            "<p>Votre demande concernant <strong>%s</strong> a été rejetée.</p>" +
            "<p>Raison: %s</p>" +
            "</body></html>",
            personName, reason != null ? reason : "Aucune raison spécifiée"
        );
        sendEmail(userEmail, subject, htmlContent);
    }
}
