package com.bibliotech.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

/**
 * Point d'accès unique à JPA.
 * L'EntityManagerFactory est créée une seule fois (coûteuse), au premier besoin,
 * un EntityManager est créé à chaque utilisation (léger, non thread-safe).
 */
public final class JpaUtil {

    private static volatile EntityManagerFactory emf;

    private JpaUtil() {}

    /**
     * Création au premier appel. Si la base est injoignable, une exception est levée
     * et on réessaiera au prochain appel (pas d'échec définitif de la classe).
     */
    private static EntityManagerFactory factory() {
        EntityManagerFactory f = emf;
        if (f == null) {
            synchronized (JpaUtil.class) {
                f = emf;
                if (f == null) {
                    f = creerFactory();
                    emf = f;
                }
            }
        }
        return f;
    }

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
        return factory().createEntityManager();
    }

    public static void close() {
        EntityManagerFactory f = emf;
        if (f != null && f.isOpen()) {
            f.close();
        }
    }
}