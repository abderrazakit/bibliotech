package com.bibliotech.repository;

import com.bibliotech.model.Livre;
import com.bibliotech.model.LivreAvecEmprunts;
import com.bibliotech.persistence.JpaUtil;
import com.bibliotech.persistence.Tx;

import java.util.List;
import java.util.Optional;

public class LivreRepository implements JpaRepository<Livre, Long> {

    @Override
    public List<Livre> findAll() {
        try (var em = JpaUtil.newEntityManager()) {
            return em.createQuery("SELECT l FROM Livre l ORDER BY l.titre", Livre.class)
                     .getResultList();
        }
    }

    @Override
    public Optional<Livre> findById(Long id) {
        try (var em = JpaUtil.newEntityManager()) {
            return Optional.ofNullable(em.find(Livre.class, id));
        }
    }

    @Override
    public Livre save(Livre livre) {
        return Tx.inTransaction(em -> {
            if (livre.getId() == null) {
                em.persist(livre);          // INSERT, l'id est rempli après
                return livre;
            }
            return em.merge(livre);         // UPDATE
        });
    }

    @Override
    public void deleteById(Long id) {
        Tx.inTransaction(em -> {
            Livre livre = em.find(Livre.class, id);
            if (livre != null) {
                em.remove(livre);
            }
        });
    }

    /** Recherche sur le titre ou l'auteur : JPQL paramétré (aucune injection possible). */
    public List<Livre> rechercher(String motCle) {
        try (var em = JpaUtil.newEntityManager()) {
            return em.createQuery("""
                    SELECT l FROM Livre l
                     WHERE LOWER(l.titre)  LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(l.auteur) LIKE LOWER(CONCAT('%', :q, '%'))
                     ORDER BY l.titre
                    """, Livre.class)
                     .setParameter("q", motCle)
                     .getResultList();
        }
    }

    /** Exercice 7.1 : livre AVEC sa collection d'emprunts chargée (JOIN FETCH). */
    public Optional<Livre> findByIdAvecEmprunts(Long id) {
        try (var em = JpaUtil.newEntityManager()) {
            return em.createQuery("""
                    SELECT l FROM Livre l
                      LEFT JOIN FETCH l.emprunts
                     WHERE l.id = :id
                    """, Livre.class)
                     .setParameter("id", id)
                     .getResultStream()
                     .findFirst();
        }
    }

    /**
     * Exercice 7.2, correction du n+1 : UNE seule requête pour tous les livres
     * ET leur nombre d'emprunts en cours. Le comptage est fait par la base
     * (COUNT + GROUP BY), aucun emprunt n'est chargé en mémoire.
     * motCle facultatif (null ou vide = tout le catalogue).
     */
    public List<LivreAvecEmprunts> findAllAvecEmpruntsEnCours(String motCle) {
        boolean filtre = motCle != null && !motCle.isBlank();

        String jpql = """
                SELECT new com.bibliotech.model.LivreAvecEmprunts(l, COUNT(e))
                  FROM Livre l
                  LEFT JOIN l.emprunts e ON e.dateRetourEffective IS NULL
                """
                + (filtre ? """
                 WHERE LOWER(l.titre)  LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(l.auteur) LIKE LOWER(CONCAT('%', :q, '%'))
                """ : "")
                + """
                 GROUP BY l.id, l.titre, l.auteur, l.anneePublication,
                          l.exemplairesTotal, l.exemplairesDisponibles
                 ORDER BY l.titre
                """;

        try (var em = JpaUtil.newEntityManager()) {
            var requete = em.createQuery(jpql, LivreAvecEmprunts.class);
            if (filtre) {
                requete.setParameter("q", motCle.strip());
            }
            return requete.getResultList();
        }
    }
}