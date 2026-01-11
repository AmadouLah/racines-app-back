package com.racines_app_back.www.service;

import com.racines_app_back.www.util.BrevoEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final BrevoEmailService brevoEmailService;

    public void sendClaimNotification(String adminEmail, String claimantName, String personName) {
        brevoEmailService.sendClaimNotification(adminEmail, claimantName, personName);
    }

    public void sendValidationRequest(String adminEmail, String requesterName, String personName) {
        brevoEmailService.sendValidationRequest(adminEmail, requesterName, personName);
    }

    public void sendApprovalNotification(String userEmail, String personName, boolean isClaim) {
        brevoEmailService.sendApprovalNotification(userEmail, personName, isClaim);
    }

    public void sendRejectionNotification(String userEmail, String personName, String reason, boolean isClaim) {
        brevoEmailService.sendRejectionNotification(userEmail, personName, reason, isClaim);
    }
}
