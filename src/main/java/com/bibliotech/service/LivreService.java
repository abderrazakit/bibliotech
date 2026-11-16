package com.bibliotech.service;

import com.bibliotech.model.Livre;
import com.bibliotech.persistence.Tx;
import com.bibliotech.repository.EmpruntRepository;
import com.bibliotech.repository.LivreRepository;

import java.util.List;
import java.util.Optional;

/** Règles métier sur les livres. La servlet ne parle qu'à ce service. */
public class LivreService {

    private final LivreRepository livreRepository;
    private final EmpruntRepository empruntRepository;

    public LivreService() {
        this(new LivreRepository(), new EmpruntRepository());
    }

    /** Constructeur utilisé par les tests (on y passe des mocks Mockito). */
    public LivreService(LivreRepository livreRepository, EmpruntRepository empruntRepository) {
        this.livreRepository = livreRepository;
        this.empruntRepository = empruntRepository;
    }

    public List<Livre> lister() {
        return livreRepository.findAll();
    }

    public List<Livre> rechercher(String motCle) {
        if (motCle == null || motCle.isBlank()) {
            return lister();
        }
        return livreRepository.rechercher(motCle.strip());
    }

    public Optional<Livre> chercher(Long id) {
        return livreRepository.findById(id);
    }

    /** Création : règle métier, un nouveau livre a tous ses exemplaires disponibles. */
    public Livre creer(Livre livre) {
        livre.setExemplairesDisponibles(livre.getExemplairesTotal());
        Validateur.valider(livre);
        return livreRepository.save(livre);          // save() ouvre sa propre transaction
    }

    /**
     * Modification : le livre est chargé PUIS modifié dans la même transaction.
     * Pas besoin d'appeler save() : Hibernate détecte les changements de l'entité
     * gérée et envoie l'UPDATE au commit ("dirty checking").
     */
    public Livre modifier(Long id, Livre nouvellesValeurs) {
        return Tx.inTransaction(em -> {
            Livre livre = em.find(Livre.class, id);
            if (livre == null) {
                throw new IllegalArgumentException("Livre introuvable : " + id);
            }
            livre.setTitre(nouvellesValeurs.getTitre());
            livre.setAuteur(nouvellesValeurs.getAuteur());
            livre.setAnneePublication(nouvellesValeurs.getAnneePublication());
            livre.setExemplairesTotal(nouvellesValeurs.getExemplairesTotal());
            livre.setExemplairesDisponibles(nouvellesValeurs.getExemplairesDisponibles());
            Validateur.valider(livre);               // si invalide : exception → rollback
            return livre;
        });
    }

    /** Suppression refusée si le livre a un historique d'emprunts (comme au TP 1). */
    public void supprimer(Long id) {
        if (empruntRepository.compterPourLivre(id) > 0) {
            throw new IllegalStateException("Impossible de supprimer ce livre : il fait l'objet d'emprunts.");
        }
        livreRepository.deleteById(id);
    }
}