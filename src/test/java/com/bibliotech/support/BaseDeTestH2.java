package com.bibliotech.support;

import com.bibliotech.model.Etudiant;
import com.bibliotech.model.Livre;
import com.bibliotech.persistence.JpaUtil;
import com.bibliotech.persistence.Tx;
import com.bibliotech.repository.EmpruntRepository;
import com.bibliotech.repository.EtudiantRepository;
import com.bibliotech.repository.LivreRepository;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base des tests d'intégration : H2 en mémoire, base vidée avant CHAQUE test.
 * Aucun test ne dépend des données d'un autre, ni de PostgreSQL.
 */
public abstract class BaseDeTestH2 {

    static {
        // Doit être fait AVANT la première utilisation de JpaUtil
        System.setProperty(JpaUtil.PROPRIETE_UNITE, "bibliotechTestPU");
    }

    protected final LivreRepository livreRepository = new LivreRepository();
    protected final EtudiantRepository etudiantRepository = new EtudiantRepository();
    protected final EmpruntRepository empruntRepository = new EmpruntRepository();

    @BeforeEach
    void viderLaBase() {
        Tx.inTransaction(em -> {
            em.createQuery("DELETE FROM Emprunt").executeUpdate();   // d'abord : il a les clés étrangères
            em.createQuery("DELETE FROM Livre").executeUpdate();
            em.createQuery("DELETE FROM Etudiant").executeUpdate();
        });
    }

    protected Livre creerLivre(String titre, String auteur, int exemplaires) {
        return livreRepository.save(new Livre(titre, auteur, 2000, exemplaires));
    }

    protected Etudiant creerEtudiant(String numero, String nom) {
        return etudiantRepository.save(
                new Etudiant(numero, nom, "Test", nom.toLowerCase() + "@univ.fr"));
    }

    protected int stock(Long livreId) {
        return livreRepository.findById(livreId).orElseThrow().getExemplairesDisponibles();
    }
}