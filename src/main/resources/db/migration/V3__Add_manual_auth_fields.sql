-- Ajout des champs pour l'authentification manuelle
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS otp_code VARCHAR(6);
ALTER TABLE users ADD COLUMN IF NOT EXISTS otp_expires_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_user_otp_code ON users(otp_code) WHERE otp_code IS NOT NULL;
