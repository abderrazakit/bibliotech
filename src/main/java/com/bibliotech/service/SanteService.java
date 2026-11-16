package com.bibliotech.service;

import com.bibliotech.persistence.JpaUtil;

/** Utilisé par /health : vérifie que la base répond. */
public class SanteService {

    /** Version de PostgreSQL ; lève une exception si la base est injoignable. */
    public String versionBase() {
        try (var em = JpaUtil.newEntityManager()) {
            return (String) em.createNativeQuery("SELECT version()").getSingleResult();
        }
    }
}