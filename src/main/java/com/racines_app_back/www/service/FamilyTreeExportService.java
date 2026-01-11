package com.racines_app_back.www.service;

import com.racines_app_back.www.domain.dto.FamilyTreeDTO;
import com.racines_app_back.www.domain.dto.PersonDTO;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FamilyTreeExportService {

    private final PersonService personService;
    
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float MARGIN = 50;
    private static final float BOX_WIDTH = 150;
    private static final float BOX_HEIGHT = 80;
    private static final float VERTICAL_SPACING = 120;
    private static final float HORIZONTAL_SPACING = 180;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] exportToPdf(UUID personId, UUID userId) throws IOException {
        FamilyTreeDTO familyTree = personService.getFamilyTree(personId, userId);
        
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                drawTree(contentStream, familyTree);
            }
            
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void drawTree(PDPageContentStream contentStream, FamilyTreeDTO familyTree) throws IOException {
        float startY = PAGE_HEIGHT - MARGIN - 50;
        float centerX = PAGE_WIDTH / 2;
        
        // Titre
        drawTitle(contentStream, centerX, startY);
        float currentY = startY - 50;
        
        // Niveau 1: Grands-parents
        if (!familyTree.getGrandparents().isEmpty()) {
            List<PersonDTO> grandparents = familyTree.getGrandparents();
            float grandparentStartX = centerX - ((grandparents.size() - 1) * HORIZONTAL_SPACING / 2);
            
            for (int i = 0; i < grandparents.size(); i++) {
                float x = grandparentStartX + (i * HORIZONTAL_SPACING);
                drawPersonBox(contentStream, grandparents.get(i), x, currentY, false);
            }
            
            // Lignes vers parents
            if (!familyTree.getParents().isEmpty()) {
                float parentY = currentY - VERTICAL_SPACING;
                drawConnectionsToChildren(contentStream, grandparents, familyTree.getParents(), 
                        grandparentStartX, currentY, parentY);
            }
            currentY -= VERTICAL_SPACING;
        }
        
        // Niveau 2: Parents
        if (!familyTree.getParents().isEmpty()) {
            List<PersonDTO> parents = familyTree.getParents();
            float parentStartX = centerX - ((parents.size() - 1) * HORIZONTAL_SPACING / 2);
            
            for (int i = 0; i < parents.size(); i++) {
                float x = parentStartX + (i * HORIZONTAL_SPACING);
                drawPersonBox(contentStream, parents.get(i), x, currentY, false);
            }
            
            // Ligne vers personne centrale
            float parentConnectionY = currentY - BOX_HEIGHT;
            drawConnectionLine(contentStream, centerX, parentConnectionY, centerX, currentY - VERTICAL_SPACING);
            currentY -= VERTICAL_SPACING;
        }
        
        // Niveau 3: Personne centrale (mise en évidence)
        PersonDTO centralPerson = familyTree.getPerson();
        drawPersonBox(contentStream, centralPerson, centerX, currentY, true);
        
        // Niveau 3: Frères et sœurs
        if (!familyTree.getSiblings().isEmpty()) {
            List<PersonDTO> siblings = familyTree.getSiblings();
            float siblingStartX = centerX + HORIZONTAL_SPACING;
            
            for (int i = 0; i < siblings.size(); i++) {
                float x = siblingStartX + (i * HORIZONTAL_SPACING);
                if (x + BOX_WIDTH / 2 < PAGE_WIDTH - MARGIN) {
                    drawPersonBox(contentStream, siblings.get(i), x, currentY, false);
                    float centerY = currentY + BOX_HEIGHT / 2;
                    drawConnectionLine(contentStream, centerX + BOX_WIDTH / 2, centerY, x - BOX_WIDTH / 2, centerY);
                }
            }
        }
    }

    private void drawTitle(PDPageContentStream contentStream, float x, float y) throws IOException {
        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        float fontSize = 20;
        contentStream.setFont(font, fontSize);
        contentStream.beginText();
        String title = "Arbre Généalogique";
        float titleWidth = font.getStringWidth(title) / 1000 * fontSize;
        contentStream.newLineAtOffset(x - titleWidth / 2, y);
        contentStream.showText(title);
        contentStream.endText();
    }

    private void drawPersonBox(PDPageContentStream contentStream, PersonDTO person, float x, float y, boolean isCentral) throws IOException {
        float boxX = x - BOX_WIDTH / 2;
        float boxY = y - BOX_HEIGHT;
        
        // Couleur de fond différente pour la personne centrale
        if (isCentral) {
            contentStream.setNonStrokingColor(0.9f, 0.9f, 1.0f);
            contentStream.addRect(boxX - 2, boxY - 2, BOX_WIDTH + 4, BOX_HEIGHT + 4);
            contentStream.fill();
            contentStream.setNonStrokingColor(0f, 0f, 0f);
        }
        
        // Bordure
        contentStream.setStrokingColor(0f, 0f, 0f);
        contentStream.setLineWidth(isCentral ? 2.0f : 1.0f);
        contentStream.addRect(boxX, boxY, BOX_WIDTH, BOX_HEIGHT);
        contentStream.stroke();
        
        // Texte
        float nameFontSize = isCentral ? 12 : 10;
        PDType1Font nameFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        contentStream.setFont(nameFont, nameFontSize);
        contentStream.beginText();
        
        // Nom et prénom
        String fullName = buildFullName(person);
        String displayName = truncateText(fullName, 18);
        float nameWidth = nameFont.getStringWidth(displayName) / 1000 * nameFontSize;
        contentStream.newLineAtOffset(x - nameWidth / 2, boxY + BOX_HEIGHT - 25);
        contentStream.showText(displayName);
        contentStream.endText();
        
        // Date de naissance
        if (person.getDateNaissance() != null) {
            PDType1Font dateFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float dateFontSize = 9;
            contentStream.setFont(dateFont, dateFontSize);
            contentStream.beginText();
            String dateText = "Né(e) : " + person.getDateNaissance().format(DATE_FORMATTER);
            float dateWidth = dateFont.getStringWidth(dateText) / 1000 * dateFontSize;
            contentStream.newLineAtOffset(x - dateWidth / 2, boxY + BOX_HEIGHT - 45);
            contentStream.showText(dateText);
            contentStream.endText();
        }
        
        // Lieu de naissance
        if (person.getLieuNaissance() != null && !person.getLieuNaissance().isEmpty()) {
            PDType1Font placeFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float placeFontSize = 8;
            contentStream.setFont(placeFont, placeFontSize);
            contentStream.beginText();
            String placeText = truncateText(person.getLieuNaissance(), 20);
            float placeWidth = placeFont.getStringWidth(placeText) / 1000 * placeFontSize;
            contentStream.newLineAtOffset(x - placeWidth / 2, boxY + BOX_HEIGHT - 60);
            contentStream.showText(placeText);
            contentStream.endText();
        }
    }

    private void drawConnectionLine(PDPageContentStream contentStream, float x1, float y1, float x2, float y2) throws IOException {
        contentStream.setStrokingColor(0f, 0f, 0f);
        contentStream.setLineWidth(1.0f);
        contentStream.moveTo(x1, y1);
        contentStream.lineTo(x2, y2);
        contentStream.stroke();
    }

    private void drawConnectionsToChildren(PDPageContentStream contentStream, 
                                          List<PersonDTO> parents, 
                                          List<PersonDTO> children, 
                                          float parentStartX, 
                                          float parentY, 
                                          float childY) throws IOException {
        if (parents.isEmpty() || children.isEmpty()) {
            return;
        }
        
        float centerX = PAGE_WIDTH / 2;
        float childStartX = centerX - ((children.size() - 1) * HORIZONTAL_SPACING / 2);
        float connectionY = parentY - BOX_HEIGHT;
        float midY = connectionY - (VERTICAL_SPACING - BOX_HEIGHT) / 2;
        
        // Lignes verticales depuis les parents
        for (int i = 0; i < parents.size(); i++) {
            float parentX = parentStartX + (i * HORIZONTAL_SPACING);
            drawConnectionLine(contentStream, parentX, connectionY, parentX, midY);
        }
        
        // Ligne horizontale
        float minX = Math.min(parentStartX, childStartX);
        float maxX = Math.max(parentStartX + ((parents.size() - 1) * HORIZONTAL_SPACING), 
                              childStartX + ((children.size() - 1) * HORIZONTAL_SPACING));
        drawConnectionLine(contentStream, minX, midY, maxX, midY);
        
        // Lignes verticales vers les enfants
        for (int i = 0; i < children.size(); i++) {
            float childX = childStartX + (i * HORIZONTAL_SPACING);
            drawConnectionLine(contentStream, childX, midY, childX, childY + BOX_HEIGHT);
        }
    }

    private String buildFullName(PersonDTO person) {
        return person.getPrenom() + " " + person.getNom();
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }
}
