package com.bibliotech.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.Year;
import org.junit.jupiter.api.Test;

class ModelValidationTest {

    // ===================== Livre =====================

    @Test
    void livreValideEstCree() {
        Livre l = new Livre(1, "  Effective Java ", "Joshua Bloch", 2018, 3, 2);
        assertEquals("Effective Java", l.titre());      // espaces retirés par strip()
        assertEquals(2, l.exemplairesDisponibles());
    }

    @Test
    void livreTitreVideOuNullEstRefuse() {
        assertThrows(IllegalArgumentException.class, () -> new Livre(1, "", "Bloch", 2018, 3, 2));
        assertThrows(IllegalArgumentException.class, () -> new Livre(1, "   ", "Bloch", 2018, 3, 2));
        assertThrows(IllegalArgumentException.class, () -> new Livre(1, null, "Bloch", 2018, 3, 2));
    }

    @Test
    void livreAnneeHorsPlageEstRefusee() {
        int anneeProchaine = Year.now().getValue() + 1;
        assertThrows(IllegalArgumentException.class, () -> new Livre(1, "T", "A", 1499, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Livre(1, "T", "A", anneeProchaine, 1, 1));
        assertDoesNotThrow(() -> new Livre(1, "T", "A", 1500, 1, 1));                     // borne basse incluse
        assertDoesNotThrow(() -> new Livre(1, "T", "A", Year.now().getValue(), 1, 1));    // borne haute incluse
    }

    @Test
    void livreDisponiblesSuperieurAuTotalEstRefuse() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new Livre(1, "T", "A", 2000, 2, 3));
        assertTrue(e.getMessage().contains("dépasser"));
    }

    // ===================== Etudiant =====================

    @Test
    void etudiantValideEstCree() {
        assertDoesNotThrow(() -> new Etudiant(1, "ET123456", "Hajji", "Abderrazak", "a@univ.fr"));
    }

    @Test
    void numeroEtudiantMalFormeEstRefuse() {
        String[] numerosInvalides = {"et123456", "ET12345", "ET1234567", "E1234567", "123456ET", " ET123456"};
        for (String numero : numerosInvalides) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Etudiant(1, numero, "Nom", "Prenom", "a@b.fr"),
                    "Le numéro « " + numero + " » aurait dû être refusé");
        }
    }

    @Test
    void emailSansArobaseEstRefuse() {
        assertThrows(IllegalArgumentException.class,
                () -> new Etudiant(1, "ET123456", "Nom", "Prenom", "pas-un-email"));
    }

    // ===================== Emprunt =====================

    @Test
    void empruntValideEstCree() {
        LocalDate aujourdhui = LocalDate.now();
        assertDoesNotThrow(() -> new Emprunt(1, 1, 1, aujourdhui, aujourdhui.plusDays(14), null,
                new StatutEmprunt.EnCours(aujourdhui.plusDays(14))));
    }

    @Test
    void empruntRetourPrevuAvantEmpruntEstRefuse() {
        LocalDate aujourdhui = LocalDate.now();
        assertThrows(IllegalArgumentException.class,
                () -> new Emprunt(1, 1, 1, aujourdhui, aujourdhui.minusDays(1), null,
                        new StatutEmprunt.EnCours(aujourdhui)));
    }

    @Test
    void empruntSansStatutEstRefuse() {
        LocalDate aujourdhui = LocalDate.now();
        assertThrows(IllegalArgumentException.class,
                () -> new Emprunt(1, 1, 1, aujourdhui, aujourdhui.plusDays(14), null, null));
    }
}