package com.racines_app_back.www.domain.repository;

import com.racines_app_back.www.domain.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PersonRepository extends JpaRepository<Person, UUID> {
    List<Person> findByIsPublicTrue();
    List<Person> findByCreatedBy(UUID createdBy);
    List<Person> findByNomAndPrenom(String nom, String prenom);
    
    @Query("SELECT p FROM Person p WHERE p.isPublic = true OR p.createdBy = :userId")
    List<Person> findPublicOrCreatedByUser(@Param("userId") UUID userId);
}
