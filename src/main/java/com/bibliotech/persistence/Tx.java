package com.bibliotech.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Exécute un traitement dans UNE transaction JPA :
 * begin → traitement → commit, ou rollback si une exception est levée.
 * L'EntityManager est fermé à la fin dans tous les cas.
 */
public final class Tx {

    private Tx() {}

    /** Version qui renvoie un résultat (ex. l'id d'un emprunt créé). */
    public static <T> T inTransaction(Function<EntityManager, T> traitement) {
        try (EntityManager em = JpaUtil.newEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            try {
                T resultat = traitement.apply(em);
                tx.commit();
                return resultat;
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    tx.rollback();          // tout ce qui a été fait est annulé
                }
                throw e;                    // l'appelant reçoit l'exception d'origine
            }
        }
    }

    /** Version sans résultat (ex. suppression, retour d'un livre). */
    public static void inTransaction(Consumer<EntityManager> traitement) {
        inTransaction(em -> {
            traitement.accept(em);
            return null;
        });
    }
}