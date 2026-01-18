package com.racines_app_back.www.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OTPService {
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final SecureRandom random = new SecureRandom();

    public String generateOTP() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    public LocalDateTime getExpiryTime() {
        return LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
    }

    public boolean isOTPValid(String storedOTP, LocalDateTime expiryTime) {
        if (storedOTP == null || expiryTime == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(expiryTime);
    }

    public boolean verifyOTP(String inputOTP, String storedOTP, LocalDateTime expiryTime) {
        return isOTPValid(storedOTP, expiryTime) && storedOTP.equals(inputOTP);
    }
}
