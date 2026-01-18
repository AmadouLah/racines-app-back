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
        if (apiKey == null || apiKey.isBlank()) {
            log.error("Clé API Brevo non configurée. Impossible d'envoyer l'email à: {}", to);
            throw new RuntimeException("Service d'email non configuré. La clé API Brevo est manquante.");
        }

        if (fromEmail == null || fromEmail.isBlank()) {
            log.error("Adresse email expéditeur non configurée. Impossible d'envoyer l'email à: {}", to);
            throw new RuntimeException("Service d'email non configuré. L'adresse email expéditeur est manquante.");
        }

        try {
            Map<String, Object> sender = new HashMap<>();
            sender.put("email", fromEmail);
            sender.put("name", "Racines");

            Map<String, Object> emailData = new HashMap<>();
            emailData.put("sender", sender);
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
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.error("Erreur HTTP lors de l'envoi de l'email à {}: {} - {}", to, e.getStatusCode(), e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email: " + e.getStatusCode(), e);
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
                        "<p>Une nouvelle revendication de profil a été soumise par <strong>%s</strong> pour le profil de <strong>%s</strong>.</p>"
                        +
                        "<p>Veuillez vous connecter à l'application pour valider ou rejeter cette demande.</p>" +
                        "</body></html>",
                claimantName, personName);
        sendEmail(adminEmail, subject, htmlContent);
    }

    public void sendValidationRequest(String adminEmail, String requesterName, String personName) {
        String subject = "Demande de validation - " + personName;
        String htmlContent = String.format(
                "<html><body>" +
                        "<h2>Demande de validation</h2>" +
                        "<p><strong>%s</strong> a demandé la validation pour ajouter <strong>%s</strong> à l'arbre généalogique public.</p>"
                        +
                        "<p>Veuillez vous connecter à l'application pour valider ou rejeter cette demande.</p>" +
                        "</body></html>",
                requesterName, personName);
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
                personName);
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
                personName, reason != null ? reason : "Aucune raison spécifiée");
        sendEmail(userEmail, subject, htmlContent);
    }

    public void sendOTP(String userEmail, String otpCode) {
        String subject = "Code d'authentification - Racines";
        String htmlContent = String.format(
                "<!DOCTYPE html>" +
                        "<html lang='fr'>" +
                        "<head>" +
                        "  <meta charset='UTF-8'>" +
                        "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                        "  <style>" +
                        "    * { margin: 0; padding: 0; box-sizing: border-box; }" +
                        "    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; line-height: 1.6; color: #333; background-color: #f5f5f5; }"
                        +
                        "    .email-container { max-width: 600px; margin: 0 auto; background-color: #ffffff; }" +
                        "    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 40px 30px; text-align: center; }"
                        +
                        "    .logo { max-width: 120px; height: auto; margin-bottom: 15px; filter: brightness(0) invert(1); }"
                        +
                        "    .app-name { color: #ffffff; font-size: 32px; font-weight: 600; letter-spacing: -0.5px; margin: 0; }"
                        +
                        "    .content { padding: 50px 40px; text-align: center; }" +
                        "    .title { color: #333333; font-size: 24px; font-weight: 600; margin-bottom: 15px; }" +
                        "    .subtitle { color: #666666; font-size: 16px; margin-bottom: 40px; }" +
                        "    .otp-container { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); border-radius: 12px; padding: 30px; margin: 30px 0; box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2); }"
                        +
                        "    .otp-label { color: #ffffff; font-size: 14px; text-transform: uppercase; letter-spacing: 2px; margin-bottom: 15px; opacity: 0.9; }"
                        +
                        "    .otp-code { color: #ffffff; font-size: 48px; font-weight: 700; letter-spacing: 12px; font-family: 'Courier New', monospace; margin: 10px 0; text-align: center; }"
                        +
                        "    .info-text { color: #666666; font-size: 14px; margin-top: 30px; line-height: 1.8; }" +
                        "    .info-text strong { color: #333333; }" +
                        "    .footer { padding: 30px 40px; background-color: #f8f9fa; text-align: center; border-top: 1px solid #e9ecef; }"
                        +
                        "    .footer-text { color: #999999; font-size: 12px; margin: 5px 0; }" +
                        "    .security-notice { color: #999999; font-size: 12px; margin-top: 20px; font-style: italic; }"
                        +
                        "  </style>" +
                        "</head>" +
                        "<body>" +
                        "  <div class='email-container'>" +
                        "    <div class='header'>" +
                        "      <div style='font-size: 48px; color: #ffffff; margin-bottom: 15px;'>🌳</div>" +
                        "      <h1 class='app-name'>Racines</h1>" +
                        "    </div>" +
                        "    <div class='content'>" +
                        "      <h2 class='title'>Code d'authentification</h2>" +
                        "      <p class='subtitle'>Votre code d'authentification à 6 chiffres</p>" +
                        "      <div class='otp-container'>" +
                        "        <div class='otp-label'>Votre code</div>" +
                        "        <div class='otp-code'>%s</div>" +
                        "      </div>" +
                        "      <div class='info-text'>" +
                        "        <p><strong>Ce code est valide pendant 10 minutes.</strong></p>" +
                        "        <p>Utilisez ce code pour compléter votre authentification sur Racines.</p>" +
                        "      </div>" +
                        "      <p class='security-notice'>Si vous n'avez pas demandé ce code, veuillez ignorer cet email.</p>"
                        +
                        "    </div>" +
                        "    <div class='footer'>" +
                        "      <p class='footer-text'><strong>Racines</strong> - Arbre Généalogique Familial</p>" +
                        "      <p class='footer-text'>Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>"
                        +
                        "    </div>" +
                        "  </div>" +
                        "</body>" +
                        "</html>",
                otpCode);
        sendEmail(userEmail, subject, htmlContent);
    }
}
