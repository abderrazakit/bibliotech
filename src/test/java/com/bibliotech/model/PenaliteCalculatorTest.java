package com.bibliotech.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PenaliteCalculatorTest {

    private static final double DELTA = 0.0001;   // tolérance pour comparer des double
    private static final LocalDate AUJOURDHUI = LocalDate.now();

    // ---------- EnCours ----------

    @Test
    void enCoursNeCoutePasDePenalite() {
        StatutEmprunt statut = new StatutEmprunt.EnCours(AUJOURDHUI.plusDays(7));
        assertEquals(0.00, PenaliteCalculator.calculerPenalite(statut), DELTA);
    }

    // ---------- Rendu ----------

    @Test
    void renduATempsNeCoutePasDePenalite() {
        StatutEmprunt statut = new StatutEmprunt.Rendu(AUJOURDHUI, false, 0);
        assertEquals(0.00, PenaliteCalculator.calculerPenalite(statut), DELTA);
    }

    @Test
    void renduEnRetardCoute50CentimesParJour() {
        StatutEmprunt statut = new StatutEmprunt.Rendu(AUJOURDHUI, true, 4);
        assertEquals(2.00, PenaliteCalculator.calculerPenalite(statut), DELTA);   // 4 × 0,50
    }

    @Test
    void renduEnRetardNEstPasPlafonne() {
        StatutEmprunt statut = new StatutEmprunt.Rendu(AUJOURDHUI, true, 100);
        assertEquals(50.00, PenaliteCalculator.calculerPenalite(statut), DELTA);  // pas de plafond (règle du sujet)
    }

    @Test
    void renduIncoherentEstRefuse() {
        // en retard mais 0 jour, ou à temps mais 3 jours : incohérent
        assertThrows(IllegalArgumentException.class, () -> new StatutEmprunt.Rendu(AUJOURDHUI, true, 0));
        assertThrows(IllegalArgumentException.class, () -> new StatutEmprunt.Rendu(AUJOURDHUI, false, 3));
    }

    // ---------- EnRetard : test paramétré ----------

    @ParameterizedTest(name = "{0} jour(s) de retard → {1} €")
    @CsvSource({
            "1,    0.50",
            "10,   5.00",
            "59,  29.50",
            "60,  30.00",   // pile au plafond
            "61,  30.00",   // juste au-dessus : plafonné
            "1000, 30.00"   // très au-dessus : plafonné
    })
    void enRetardCoute50CentimesParJourPlafonneA30(long jours, double penaliteAttendue) {
        StatutEmprunt statut = new StatutEmprunt.EnRetard(AUJOURDHUI.minusDays(jours), jours);
        assertEquals(penaliteAttendue, PenaliteCalculator.calculerPenalite(statut), DELTA);
    }

    @Test
    void enRetardAvecZeroJourEstRefuse() {
        assertThrows(IllegalArgumentException.class, () -> new StatutEmprunt.EnRetard(AUJOURDHUI, 0));
    }

    // ---------- null ----------

    @Test
    void statutNullEstRefuse() {
        assertThrows(IllegalArgumentException.class, () -> PenaliteCalculator.calculerPenalite(null));
    }
}