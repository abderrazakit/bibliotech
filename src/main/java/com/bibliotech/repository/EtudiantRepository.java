package com.bibliotech.repository;

import com.bibliotech.model.Etudiant;
import com.bibliotech.persistence.JpaUtil;
import com.bibliotech.persistence.Tx;

import java.util.List;
import java.util.Optional;

public class EtudiantRepository implements JpaRepository<Etudiant, Long> {

    @Override
    public List<Etudiant> findAll() {
        try (var em = JpaUtil.newEntityManager()) {
            return em.createQuery("SELECT e FROM Etudiant e ORDER BY e.nom, e.prenom", Etudiant.class)
                     .getResultList();
        }
    }

    @Override
    public Optional<Etudiant> findById(Long id) {
        try (var em = JpaUtil.newEntityManager()) {
            return Optional.ofNullable(em.find(Etudiant.class, id));
        }
    }

    @Override
    public Etudiant save(Etudiant etudiant) {
        return Tx.inTransaction(em -> {
            if (etudiant.getId() == null) {
                em.persist(etudiant);
                return etudiant;
            }
            return em.merge(etudiant);
        });
    }

    @Override
    public void deleteById(Long id) {
        Tx.inTransaction(em -> {
            Etudiant etudiant = em.find(Etudiant.class, id);
            if (etudiant != null) {
                em.remove(etudiant);
            }
        });
    }
}