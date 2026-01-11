-- Index supplémentaires pour améliorer les performances des requêtes récursives
-- Index composite pour les recherches fréquentes
CREATE INDEX IF NOT EXISTS idx_person_public_created ON persons(is_public, created_at);
CREATE INDEX IF NOT EXISTS idx_claim_user_status ON profile_claims(user_id, status);
CREATE INDEX IF NOT EXISTS idx_pending_user_status ON pending_additions(requested_by, status);
CREATE INDEX IF NOT EXISTS idx_sync_user_status ON sync_queue(user_id, status);
