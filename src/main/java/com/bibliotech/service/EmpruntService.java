package com.bibliotech.service;

import com.bibliotech.model.Emprunt;
import com.bibliotech.model.Etudiant;
import com.bibliotech.model.Livre;
import com.bibliotech.model.LivreIndisponibleException;
import com.bibliotech.model.PenaliteCalculator;
import com.bibliotech.persistence.Tx;
import com.bibliotech.repository.EmpruntRepository;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.List;

/** Opérations métier sur les emprunts : c'est ICI que s'ouvrent les transactions. */
public class EmpruntService {

    private final EmpruntRepository empruntRepository;

    /** Bonus 1 : pause entre la vérification et la mise à jour (0 en production, réglée par les tests). */
    int pauseApresVerificationMs = 0;

    public EmpruntService() {
        this(new EmpruntRepository());
    }

    public EmpruntService(EmpruntRepository empruntRepository) {
        this.empruntRepository = empruntRepository;
    }

    /**
     * Enregistre un emprunt :
     *   1. verrouille le livre et vérifie qu'il reste un exemplaire
     *   2. décrémente le stock
     *   3. crée l'emprunt
     * Tout dans UNE transaction : tout réussit ou tout est annulé.
     */
    public Long enregistrerEmprunt(Long livreId, Long etudiantId, int dureeJours) {
        if (dureeJours <= 0) {
            throw new IllegalArgumentException("La durée d'emprunt doit être positive.");
        }
        return Tx.inTransaction(em -> {
            // PESSIMISTIC_WRITE = SELECT ... FOR UPDATE : un 2e emprunt simultané du même
            // livre attend la fin de cette transaction (correction du Bonus 1 du TP 1)
            Livre livre = em.find(Livre.class, livreId, LockModeType.PESSIMISTIC_WRITE);
            Etudiant etudiant = em.find(Etudiant.class, etudiantId);

            if (livre == null) {
                throw new IllegalArgumentException("Livre inconnu.");
            }
            if (etudiant == null) {
                throw new IllegalArgumentException("Étudiant inconnu.");
            }
            if (livre.getExemplairesDisponibles() <= 0) {
                throw new LivreIndisponibleException(livreId);
            }
            pauseDeTest();

            livre.setExemplairesDisponibles(livre.getExemplairesDisponibles() - 1);   // → UPDATE livre

            LocalDate aujourdhui = LocalDate.now();
            Emprunt emprunt = new Emprunt(livre, etudiant, aujourdhui, aujourdhui.plusDays(dureeJours));
            Validateur.valider(emprunt);
            em.persist(emprunt);                                                      // → INSERT emprunt
            em.flush();                // envoie les requêtes maintenant pour obtenir l'id généré
            return emprunt.getId();
        });
    }

    /**
     * Enregistre le retour d'un livre :
     *   1. verrouille l'emprunt (empêche un double retour simultané)
     *   2. renseigne la date de retour et le statut
     *   3. ré-incrémente le stock du livre
     */
    public void enregistrerRetour(Long empruntId) {
        Tx.inTransaction(em -> {
            Emprunt emprunt = em.find(Emprunt.class, empruntId, LockModeType.PESSIMISTIC_WRITE);
            if (emprunt == null) {
                throw new IllegalArgumentException("Emprunt inconnu.");
            }
            if (emprunt.getDateRetourEffective() != null) {
                throw new IllegalStateException("Ce livre a déjà été rendu.");
            }
            emprunt.setDateRetourEffective(LocalDate.now());
            // Seuls EN_COURS et RENDU sont stockés : un retour en retard se déduit des dates
            emprunt.setStatut(Emprunt.Statut.RENDU);

            Livre livre = emprunt.getLivre();      // chargement LAZY : OK, on est dans la transaction
            livre.setExemplairesDisponibles(livre.getExemplairesDisponibles() + 1);
        });
    }

    /** Emprunts non rendus, avec livre et étudiant déjà chargés (JOIN FETCH). */
    public List<Emprunt> listerEnCours() {
        return empruntRepository.findEnCours();
    }

    public List<Emprunt> listerEnRetard() {
        return empruntRepository.findEnRetard();
    }

    /** Pénalité d'un emprunt : on réutilise le PenaliteCalculator du TP 1. */
    public double penalite(Emprunt emprunt) {
        return PenaliteCalculator.calculerPenalite(emprunt.statutMetier());
    }

    private void pauseDeTest() {
        if (pauseApresVerificationMs > 0) {
            try {
                Thread.sleep(pauseApresVerificationMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}