package com.bibliotech.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

/**
 * Point d'accès unique à JPA.
 * L'EntityManagerFactory est créée une seule fois (coûteuse),
 * un EntityManager est créé à chaque utilisation (léger, non thread-safe).
 */
public final class JpaUtil {

    private static final EntityManagerFactory EMF = creerFactory();

    private JpaUtil() {}

    private static EntityManagerFactory creerFactory() {
        // Hibernate ne lit pas ${env.X} dans persistence.xml :
        // on passe les identifiants ici, depuis les variables d'environnement.
        Map<String, String> props = new HashMap<>();
        props.put("jakarta.persistence.jdbc.url",      exiger("DB_URL"));
        props.put("jakarta.persistence.jdbc.user",     exiger("DB_USER"));
        props.put("jakarta.persistence.jdbc.password", exiger("DB_PASSWORD"));
        return Persistence.createEntityManagerFactory("bibliotechPU", props);
    }

    private static String exiger(String nom) {
        String valeur = System.getenv(nom);
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalStateException("Variable d'environnement manquante : " + nom);
        }
        return valeur;
    }

    public static EntityManager newEntityManager() {
        return EMF.createEntityManager();
    }

    public static void close() {
        if (EMF.isOpen()) {
            EMF.close();
        }
    }
}