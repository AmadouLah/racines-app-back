-- Table: users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    nom VARCHAR(255) NOT NULL,
    prenom VARCHAR(255) NOT NULL,
    date_naissance DATE NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'PENDING_USER',
    person_id UUID,
    oauth2_provider_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_person_id ON users(person_id);

-- Table: persons
CREATE TABLE persons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom VARCHAR(255) NOT NULL,
    prenom VARCHAR(255) NOT NULL,
    date_naissance DATE,
    lieu_naissance VARCHAR(255),
    is_public BOOLEAN NOT NULL DEFAULT false,
    created_by UUID,
    validated_by UUID,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_person_nom_prenom ON persons(nom, prenom);
CREATE INDEX idx_person_is_public ON persons(is_public);
CREATE INDEX idx_person_created_by ON persons(created_by);

-- Table: family_relationships
CREATE TABLE family_relationships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    person1_id UUID NOT NULL,
    person2_id UUID NOT NULL,
    relationship_type VARCHAR(50) NOT NULL,
    side VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN',
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_person1 FOREIGN KEY (person1_id) REFERENCES persons(id) ON DELETE CASCADE,
    CONSTRAINT fk_person2 FOREIGN KEY (person2_id) REFERENCES persons(id) ON DELETE CASCADE,
    CONSTRAINT unique_relationship UNIQUE (person1_id, person2_id, relationship_type)
);

CREATE INDEX idx_relationship_person1 ON family_relationships(person1_id);
CREATE INDEX idx_relationship_person2 ON family_relationships(person2_id);
CREATE INDEX idx_relationship_type ON family_relationships(relationship_type);

-- Table: profile_claims
CREATE TABLE profile_claims (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    person_id UUID NOT NULL,
    user_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    nom VARCHAR(255) NOT NULL,
    prenom VARCHAR(255) NOT NULL,
    date_naissance DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    processed_by UUID,
    processed_at TIMESTAMP,
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_claim_person FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE,
    CONSTRAINT fk_claim_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_claim_person_id ON profile_claims(person_id);
CREATE INDEX idx_claim_user_id ON profile_claims(user_id);
CREATE INDEX idx_claim_status ON profile_claims(status);

-- Table: pending_additions
CREATE TABLE pending_additions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    person_id UUID NOT NULL,
    requested_by UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    processed_by UUID,
    processed_at TIMESTAMP,
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_pending_person FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE,
    CONSTRAINT fk_pending_user FOREIGN KEY (requested_by) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_pending_person_id ON pending_additions(person_id);
CREATE INDEX idx_pending_requested_by ON pending_additions(requested_by);
CREATE INDEX idx_pending_status ON pending_additions(status);

-- Table: sync_queue
CREATE TABLE sync_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID,
    payload TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    synced_at TIMESTAMP,
    CONSTRAINT fk_sync_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_sync_user_id ON sync_queue(user_id);
CREATE INDEX idx_sync_status ON sync_queue(status);
CREATE INDEX idx_sync_entity_type ON sync_queue(entity_type);
