package com.bibliotech.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.bibliotech.model.Livre;
import com.bibliotech.model.LivreIndisponibleException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Bonus 1 : deux étudiants empruntent EN MÊME TEMPS le dernier exemplaire d'un livre.
 * Comportement attendu : un seul emprunt réussit, le stock finit à 0 (jamais -1).
 */
class RaceConditionTest {

    private static final long ALICE = 1;   // ET123456 (schema.sql)
    private static final long BOB   = 2;   // ET234567
    private static final String TITRE_TEST = "Livre test race condition";

    private final EmpruntDAO empruntDAO = new EmpruntDAO();
    private final LivreDAO livreDAO = new LivreDAO();
    private long livreId;

    @BeforeAll
    static void baseDisponible() {
        assumeTrue(System.getenv("DB_URL") != null, "Variables DB_* absentes : tests ignorés");
    }

    /** Crée un livre dédié au test, avec UN SEUL exemplaire. */
    @BeforeEach
    void creerLivreAvecUnExemplaire() {
        livreDAO.save(new Livre(0, TITRE_TEST, "Test", 2020, 1, 1));
        livreId = livreDAO.findAll().stream()
                .filter(l -> l.titre().equals(TITRE_TEST))
                .findFirst().orElseThrow().id();
    }

    /** Supprime les emprunts et le livre créés par le test. */
    @AfterEach
    void nettoyer() throws Exception {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement emprunts = c.prepareStatement("DELETE FROM emprunt WHERE livre_id = ?");
             PreparedStatement livre = c.prepareStatement("DELETE FROM livre WHERE id = ?")) {
            emprunts.setLong(1, livreId);
            emprunts.executeUpdate();
            livre.setLong(1, livreId);
            livre.executeUpdate();
        }
    }

    @Test
    void deuxEmpruntsSimultanesSurLeDernierExemplaire() throws Exception {
        empruntDAO.pauseApresVerificationMs = 300;   // élargit la fenêtre de concurrence

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch depart = new CountDownLatch(1);           // « coup de pistolet » du départ
        List<String> resultats = new CopyOnWriteArrayList<>();   // liste thread-safe

        pool.submit(() -> emprunter("Alice", ALICE, depart, resultats));
        pool.submit(() -> emprunter("Bob",   BOB,   depart, resultats));

        depart.countDown();                                      // les 2 threads partent ENSEMBLE
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS), "Les threads n'ont pas terminé");

        int stockFinal = lireStock();
        long succes = resultats.stream().filter(r -> r.endsWith("SUCCÈS")).count();

        System.out.println("=== Bonus 1 : race condition ===");
        resultats.forEach(System.out::println);
        System.out.println("Stock final = " + stockFinal + " | emprunts réussis = " + succes);

        assertEquals(1, succes, "Un seul des deux emprunts aurait dû réussir");
        assertEquals(0, stockFinal, "Le stock ne doit jamais devenir négatif");
    }

    /** Ce que fait chaque thread : attendre le départ, puis tenter l'emprunt. */
    private void emprunter(String nom, long etudiantId, CountDownLatch depart, List<String> resultats) {
        try {
            depart.await();
            empruntDAO.enregistrerEmprunt(livreId, etudiantId, 14);
            resultats.add(nom + " : SUCCÈS");
        } catch (LivreIndisponibleException e) {
            resultats.add(nom + " : refusé (LivreIndisponibleException)");
        } catch (DAOException e) {
            resultats.add(nom + " : refusé (DAOException : " + e.getCause().getMessage() + ")");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Lit le stock directement en SQL : un Livre avec un stock de -1 serait refusé
     * par le constructeur du record (ValidationException).
     */
    private int lireStock() throws Exception {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT exemplaires_disponibles FROM livre WHERE id = ?")) {
            ps.setLong(1, livreId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}