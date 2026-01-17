package com.racines_app_back.www.controller;

import com.racines_app_back.www.domain.dto.ApiResponse;
import com.racines_app_back.www.domain.dto.FamilyTreeDTO;
import com.racines_app_back.www.domain.dto.PersonCreateDTO;
import com.racines_app_back.www.domain.dto.PersonDTO;
import com.racines_app_back.www.domain.dto.PersonUpdateDTO;
import com.racines_app_back.www.domain.dto.RelationshipDTO;
import com.racines_app_back.www.domain.enums.RelationshipType;
import com.racines_app_back.www.service.CurrentUserService;
import com.racines_app_back.www.service.FamilyTreeExportService;
import com.racines_app_back.www.service.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;
    private final CurrentUserService currentUserService;
    private final FamilyTreeExportService familyTreeExportService;

    @GetMapping("/public/tree")
    public ResponseEntity<ApiResponse<FamilyTreeDTO>> getPublicTree() {
        FamilyTreeDTO familyTree = personService.getPublicTree();
        if (familyTree == null) {
            return ResponseEntity.ok(ApiResponse.error("Aucun arbre public disponible"));
        }
        return ResponseEntity.ok(ApiResponse.success(familyTree));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PersonDTO>> getPerson(@PathVariable UUID id) {
        UUID userId = currentUserService.getCurrentUserId();
        PersonDTO person = personService.getPersonById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(person));
    }

    @GetMapping("/{id}/family-tree")
    public ResponseEntity<ApiResponse<FamilyTreeDTO>> getFamilyTree(@PathVariable UUID id) {
        UUID userId = currentUserService.getCurrentUserId();
        FamilyTreeDTO familyTree = personService.getFamilyTree(id, userId);
        return ResponseEntity.ok(ApiResponse.success(familyTree));
    }

    @GetMapping("/{id}/family-tree/export")
    public ResponseEntity<Resource> exportFamilyTree(@PathVariable UUID id) throws IOException {
        UUID userId = currentUserService.getCurrentUserId();
        byte[] pdfContent = familyTreeExportService.exportToPdf(id, userId);
        
        PersonDTO person = personService.getPersonById(id, userId);
        String filename = String.format("arbre-%s-%s-%s.pdf", 
                person.getNom(), person.getPrenom(), id.toString().substring(0, 8));
        
        ByteArrayResource resource = new ByteArrayResource(pdfContent);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfContent.length)
                .body(resource);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PersonDTO>> createPerson(@Valid @RequestBody PersonCreateDTO dto) {
        UUID userId = currentUserService.getCurrentUserId();
        PersonDTO person = personService.createPerson(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Personne créée avec succès", person));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PersonDTO>> updatePerson(
            @PathVariable UUID id,
            @Valid @RequestBody PersonUpdateDTO dto) {
        UUID userId = currentUserService.getCurrentUserId();
        PersonDTO person = personService.updatePerson(id, dto, userId);
        return ResponseEntity.ok(ApiResponse.success("Personne mise à jour avec succès", person));
    }

    @PostMapping("/{id}/relationships")
    public ResponseEntity<ApiResponse<RelationshipDTO>> addRelationship(
            @PathVariable UUID id,
            @RequestParam UUID relatedPersonId,
            @RequestParam RelationshipType relationshipType) {
        UUID userId = currentUserService.getCurrentUserId();
        RelationshipDTO relationship = personService.addRelationship(id, relatedPersonId, relationshipType, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Relation ajoutée avec succès", relationship));
    }

    @GetMapping("/{id}/ancestors")
    public ResponseEntity<ApiResponse<List<PersonDTO>>> getAncestors(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "5") int maxDepth) {
        UUID userId = currentUserService.getCurrentUserId();
        List<PersonDTO> ancestors = personService.getAncestors(id, userId, maxDepth);
        return ResponseEntity.ok(ApiResponse.success(ancestors));
    }

    @GetMapping("/{id}/descendants")
    public ResponseEntity<ApiResponse<List<PersonDTO>>> getDescendants(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "5") int maxDepth) {
        UUID userId = currentUserService.getCurrentUserId();
        List<PersonDTO> descendants = personService.getDescendants(id, userId, maxDepth);
        return ResponseEntity.ok(ApiResponse.success(descendants));
    }
}
