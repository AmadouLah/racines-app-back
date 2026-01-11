package com.racines_app_back.www.domain.entity;

import com.racines_app_back.www.domain.enums.RelationshipType;
import com.racines_app_back.www.domain.enums.Side;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "family_relationships", indexes = {
    @Index(name = "idx_relationship_person1", columnList = "person1_id"),
    @Index(name = "idx_relationship_person2", columnList = "person2_id"),
    @Index(name = "idx_relationship_type", columnList = "relationship_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "person1_id", nullable = false)
    private UUID person1Id;

    @Column(name = "person2_id", nullable = false)
    private UUID person2Id;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false)
    private RelationshipType relationshipType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Side side = Side.UNKNOWN;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
