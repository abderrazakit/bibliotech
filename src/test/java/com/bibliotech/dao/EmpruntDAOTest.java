package com.bibliotech.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.bibliotech.model.Emprunt;
import com.bibliotech.model.StatutEmprunt;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests d'intégration : nécessitent la base "bibliotech" initialisée avec sql/schema.sql
 * et les variables DB_URL / DB_USER / DB_PASSWORD. Ignorés sinon.
 */
class EmpruntDAOTest {

    private final EmpruntDAO dao = new EmpruntDAO();

    @BeforeAll
    static void baseDisponible() {
        assumeTrue(System.getenv("DB_URL") != null, "Variables DB_* absentes : tests ignorés");
    }

    @Test
    void findAllRetourneLes3EmpruntsDeDemo() {
        assertEquals(3, dao.findAll().size());
    }

    @Test
    void findEnRetardRetourneUniquementLesMiserables() {
        List<Emprunt> enRetard = dao.findEnRetard();
        assertEquals(1, enRetard.size());
        Emprunt e = enRetard.get(0);
        assertNull(e.dateRetourEffective());
        assertTrue(e.dateRetourPrevue().isBefore(LocalDate.now()));
        // Le statut calculé est bien EnRetard, avec 5 jours de retard
        StatutEmprunt.EnRetard statut = assertInstanceOf(StatutEmprunt.EnRetard.class, e.statut());
        assertEquals(5, statut.joursDeRetard());
    }

    @Test
    void findEnCoursRetourneLesEmpruntsNonRendus() {
        assertEquals(2, dao.findEnCours().size());   // Clean Code (en cours) + Les Misérables (en retard)
    }

    @Test
    void calculDuStatutSelonLesDates() {
        LocalDate aujourdhui = LocalDate.now();
        assertInstanceOf(StatutEmprunt.EnCours.class,
                EmpruntDAO.calculerStatut(aujourdhui.plusDays(3), null));
        assertInstanceOf(StatutEmprunt.EnRetard.class,
                EmpruntDAO.calculerStatut(aujourdhui.minusDays(3), null));
        StatutEmprunt.Rendu rendu = assertInstanceOf(StatutEmprunt.Rendu.class,
                EmpruntDAO.calculerStatut(aujourdhui.minusDays(10), aujourdhui.minusDays(7)));
        assertEquals(3, rendu.joursDeRetard());
    }
}