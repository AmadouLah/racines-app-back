package com.racines_app_back.www.domain.repository;

import com.racines_app_back.www.domain.entity.FamilyRelationship;
import com.racines_app_back.www.domain.enums.RelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FamilyRelationshipRepository extends JpaRepository<FamilyRelationship, UUID> {
    List<FamilyRelationship> findByPerson1Id(UUID personId);
    List<FamilyRelationship> findByPerson2Id(UUID personId);
    
    @Query("SELECT fr FROM FamilyRelationship fr WHERE fr.person1Id = :personId OR fr.person2Id = :personId")
    List<FamilyRelationship> findAllRelationshipsByPersonId(@Param("personId") UUID personId);
    
    List<FamilyRelationship> findByPerson1IdAndRelationshipType(UUID personId, RelationshipType type);
    List<FamilyRelationship> findByPerson2IdAndRelationshipType(UUID personId, RelationshipType type);
    
    @Query("SELECT fr FROM FamilyRelationship fr WHERE " +
           "(fr.person1Id = :person1Id AND fr.person2Id = :person2Id) OR " +
           "(fr.person1Id = :person2Id AND fr.person2Id = :person1Id)")
    List<FamilyRelationship> findRelationshipBetweenPersons(
        @Param("person1Id") UUID person1Id, 
        @Param("person2Id") UUID person2Id
    );
    
    Optional<FamilyRelationship> findByPerson1IdAndPerson2IdAndRelationshipType(
        UUID person1Id, UUID person2Id, RelationshipType type
    );
}
