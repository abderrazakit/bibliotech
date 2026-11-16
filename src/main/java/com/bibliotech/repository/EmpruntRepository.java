package com.bibliotech.repository;

import com.bibliotech.model.Emprunt;
import com.bibliotech.persistence.JpaUtil;
import com.bibliotech.persistence.Tx;

import java.util.List;
import java.util.Optional;

public class EmpruntRepository implements JpaRepository<Emprunt, Long> {

    @Override
    public List<Emprunt> findAll() {
        try (var em = JpaUtil.newEntityManager()) {
            return em.createQuery("""
                    SELECT e FROM Emprunt e
                      JOIN FETCH e.livre
                      JOIN FETCH e.etudiant
                     ORDER BY e.dateEmprunt DESC, e.id DESC
                    """, Emprunt.class)
                     .getResultList();
        }
    }

    @Override
    public Optional<Emprunt> findById(Long id) {
        try (var em = JpaUtil.newEntityManager()) {
            return Optional.ofNullable(em.find(Emprunt.class, id));
        }
    }

    @Override
    public Emprunt save(Emprunt emprunt) {
        return Tx.inTransaction(em -> {
            if (emprunt.getId() == null) {
                em.persist(emprunt);
                return emprunt;
            }
            return em.merge(emprunt);
        });
    }

    @Override
    public void deleteById(Long id) {
        Tx.inTransaction(em -> {
            Emprunt emprunt = em.find(Emprunt.class, id);
            if (emprunt != null) {
                em.remove(emprunt);
            }
        });
    }

    /** Tous les emprunts non rendus (en cours ou en retard), avec livre et étudiant chargés. */
    public List<Emprunt> findEnCours() {
        try (var em = JpaUtil.newEntityManager()) {
            return em.createQuery("""
                    SELECT e FROM Emprunt e
                      JOIN FETCH e.livre
                      JOIN FETCH e.etudiant
                     WHERE e.dateRetourEffective IS NULL
                     ORDER BY e.dateRetourPrevue
                    """, Emprunt.class)
                     .getResultList();
        }
    }

    /** Emprunts non rendus d'un étudiant (demandé par le sujet). */
    public List<Emprunt> findEnCoursPourEtudiant(Long etudiantId) {
        try (var em = JpaUtil.newEntityManager()) {
            return em.createQuery("""
                    SELECT e FROM Emprunt e
                      JOIN FETCH e.livre
                     WHERE e.etudiant.id = :etudiantId
                       AND e.dateRetourEffective IS NULL
                     ORDER BY e.dateRetourPrevue
                    """, Emprunt.class)
                     .setParameter("etudiantId", etudiantId)
                     .getResultList();
        }
    }
    
    /** Nombre d'emprunts (passés ou en cours) d'un livre. */
    public long compterPourLivre(Long livreId) {
        try (var em = JpaUtil.newEntityManager()) {
            return em.createQuery(
                    "SELECT COUNT(e) FROM Emprunt e WHERE e.livre.id = :livreId", Long.class)
                     .setParameter("livreId", livreId)
                     .getSingleResult();
        }
    }

    /** Emprunts en retard : date prévue dépassée et livre pas encore rendu. */
    public List<Emprunt> findEnRetard() {
        try (var em = JpaUtil.newEntityManager()) {
            return em.createQuery("""
                    SELECT e FROM Emprunt e
                      JOIN FETCH e.livre
                      JOIN FETCH e.etudiant
                     WHERE e.dateRetourPrevue < CURRENT_DATE
                       AND e.dateRetourEffective IS NULL
                    """, Emprunt.class)
                     .getResultList();
        }
    }
}