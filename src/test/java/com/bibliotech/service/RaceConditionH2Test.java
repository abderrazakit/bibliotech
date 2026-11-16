package com.bibliotech.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bibliotech.model.Etudiant;
import com.bibliotech.model.Livre;
import com.bibliotech.model.LivreIndisponibleException;
import com.bibliotech.support.BaseDeTestH2;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Deux étudiants empruntent EN MÊME TEMPS le dernier exemplaire.
 * Grâce à PESSIMISTIC_WRITE (SELECT ... FOR UPDATE), Bob attend qu'Alice ait fini,
 * puis voit 0 exemplaire : il reçoit une LivreIndisponibleException propre.
 */
class RaceConditionH2Test extends BaseDeTestH2 {

    @Test
    void deux_emprunts_simultanes_sur_le_dernier_exemplaire() throws Exception {
        Livre livre = creerLivre("Livre test race condition", "Test", 1);
        Etudiant alice = creerEtudiant("ET123456", "Martin");
        Etudiant bob = creerEtudiant("ET234567", "Durand");

        EmpruntService service = new EmpruntService();
        service.pauseApresVerificationMs = 300;              // élargit la fenêtre de concurrence

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch depart = new CountDownLatch(1);
        List<String> resultats = new CopyOnWriteArrayList<>();

        pool.submit(() -> emprunter(service, "Alice", livre.getId(), alice.getId(), depart, resultats));
        pool.submit(() -> emprunter(service, "Bob",   livre.getId(), bob.getId(),   depart, resultats));

        depart.countDown();                                   // les 2 threads partent ensemble
        pool.shutdown();
        assertThat(pool.awaitTermination(15, TimeUnit.SECONDS)).isTrue();

        System.out.println("=== Race condition (JPA + PESSIMISTIC_WRITE) ===");
        resultats.forEach(System.out::println);
        System.out.println("Stock final = " + stock(livre.getId()));

        assertThat(resultats).filteredOn(r -> r.endsWith("SUCCÈS")).hasSize(1);
        assertThat(resultats).filteredOn(r -> r.endsWith("refusé (LivreIndisponibleException)")).hasSize(1);
        assertThat(stock(livre.getId())).isZero();
        assertThat(empruntRepository.compterPourLivre(livre.getId())).isEqualTo(1);
    }

    private void emprunter(EmpruntService service, String nom, Long livreId, Long etudiantId,
                           CountDownLatch depart, List<String> resultats) {
        try {
            depart.await();
            service.enregistrerEmprunt(livreId, etudiantId, 14);
            resultats.add(nom + " : SUCCÈS");
        } catch (LivreIndisponibleException e) {
            resultats.add(nom + " : refusé (LivreIndisponibleException)");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            resultats.add(nom + " : ERREUR " + e);           // ne devrait pas arriver
        }
    }
}