package com.bibliotech.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.bibliotech.model.Emprunt;
import com.bibliotech.model.Livre;
import com.bibliotech.model.LivreIndisponibleException;
import com.bibliotech.model.StatutEmprunt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Tests d'intégration de la transaction enregistrerEmprunt (C.3). */
class EnregistrerEmpruntTest {

    private static final long ETUDIANT_ALICE = 1;          // ET123456 (schema.sql)
    private static final long ETUDIANT_INEXISTANT = 999_999;

    private final EmpruntDAO empruntDAO = new EmpruntDAO();
    private final LivreDAO livreDAO = new LivreDAO();

    @BeforeAll
    static void baseDisponible() {
        assumeTrue(System.getenv("DB_URL") != null, "Variables DB_* absentes : tests ignorés");
    }

    /** Remet la base dans l'état de schema.sql après chaque test. */
    @AfterEach
    void nettoyer() throws Exception {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement supprimer = c.prepareStatement(
                     "DELETE FROM emprunt WHERE date_emprunt = CURRENT_DATE");
             PreparedStatement recalculer = c.prepareStatement(
                     "UPDATE livre l SET exemplaires_disponibles = l.exemplaires_total - "
                   + "(SELECT COUNT(*) FROM emprunt e WHERE e.livre_id = l.id "
                   + " AND e.date_retour_effective IS NULL)")) {
            supprimer.executeUpdate();
            recalculer.executeUpdate();
        }
    }

    private long idLivre(String titre) {
        return livreDAO.findAll().stream()
                .filter(l -> l.titre().equals(titre))
                .findFirst().orElseThrow().id();
    }

    private int disponibles(long livreId) {
        return livreDAO.findById(livreId).map(Livre::exemplairesDisponibles).orElseThrow();
    }

    @Test
    void empruntValideDecrementeLeStockEtCreeLEmprunt() {
        long livre = idLivre("Design Patterns");                 // 2 disponibles
        int stockAvant = disponibles(livre);
        int enCoursAvant = empruntDAO.findEnCours().size();

        empruntDAO.enregistrerEmprunt(livre, ETUDIANT_ALICE, 14);

        assertEquals(stockAvant - 1, disponibles(livre));          // étape 2 : stock décrémenté
        assertEquals(enCoursAvant + 1, empruntDAO.findEnCours().size()); // étape 3 : emprunt créé

        Emprunt cree = empruntDAO.findEnCours().stream()
                .filter(e -> e.livreId() == livre && e.dateEmprunt().equals(LocalDate.now()))
                .findFirst().orElseThrow();
        assertEquals(LocalDate.now().plusDays(14), cree.dateRetourPrevue());
        assertInstanceOf(StatutEmprunt.EnCours.class, cree.statut());
    }

    @Test
    void livreSansExemplaireLeveLivreIndisponibleException() {
        long livre = idLivre("Les Misérables");                   // 0 disponible
        int enCoursAvant = empruntDAO.findEnCours().size();

        assertThrows(LivreIndisponibleException.class,
                () -> empruntDAO.enregistrerEmprunt(livre, ETUDIANT_ALICE, 14));

        assertEquals(0, disponibles(livre));                      // stock inchangé
        assertEquals(enCoursAvant, empruntDAO.findEnCours().size()); // aucun emprunt créé
    }

    @Test
    void echecALInsertAnnuleLaDecrementationRollback() {
        long livre = idLivre("Design Patterns");
        int stockAvant = disponibles(livre);

        // L'étape 2 (UPDATE) réussit, puis l'étape 3 (INSERT) échoue :
        // la clé étrangère refuse un étudiant inexistant.
        assertThrows(DAOException.class,
                () -> empruntDAO.enregistrerEmprunt(livre, ETUDIANT_INEXISTANT, 14));

        // Grâce au ROLLBACK, la décrémentation de l'étape 2 est annulée
        assertEquals(stockAvant, disponibles(livre));
    }

    @Test
    void livreInexistantEstRefuse() {
        assertThrows(IllegalArgumentException.class,
                () -> empruntDAO.enregistrerEmprunt(999_999, ETUDIANT_ALICE, 14));
    }
}